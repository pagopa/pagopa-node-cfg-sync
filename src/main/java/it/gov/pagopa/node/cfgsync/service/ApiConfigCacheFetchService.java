package it.gov.pagopa.node.cfgsync.service;

import feign.Feign;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Setter
@Slf4j
@RequiredArgsConstructor
public class ApiConfigCacheFetchService {

    @Value("${api-config-cache.service.host}")
    private String apiConfigCacheUrl;

    @Value("${api-config-cache.service.subscriptionKey}")
    private String apiConfigCacheSubscriptionKey;

    @Value("${api-config-cache.service.retryLeft}")
    private Integer retryLeft;

    @Value("${api-config-cache.service.attemptDelay}")
    private Integer attempt;

    private ApiConfigCacheClient apiConfigCacheClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    private void setApiConfigCacheClient() {
        apiConfigCacheClient = Feign.builder().target(ApiConfigCacheClient.class, apiConfigCacheUrl);
    }

    public CompletableFuture<Response> fetchCacheWithRetry() {
        return fetchWithRetry(retryLeft, attempt);
    }

    private CompletableFuture<Response> fetchWithRetry(int retryLeft, int attempt) {
        log.info("Fetching cache with {} attempt(s) in {} sec", attempt, Math.pow(2, attempt-1));
        return CompletableFuture.supplyAsync(() -> apiConfigCacheClient.getCache(apiConfigCacheSubscriptionKey))
                .thenCompose(response -> {
                    if (response.status() == HttpStatus.OK.value()) {
                        return CompletableFuture.completedFuture(response);
                    } else if (retryLeft > 0) {
                        long delay = (long) Math.pow(2, attempt);
                        CompletableFuture<Response> retryFuture = new CompletableFuture<>();
                        scheduler.schedule(
                                () -> fetchWithRetry(retryLeft - 1, attempt + 1)
                                        .whenComplete((res, ex) -> {
                                            if (ex != null) retryFuture.completeExceptionally(ex);
                                            else retryFuture.complete(res);
                                        }),
                                delay, TimeUnit.SECONDS
                        );
                        return retryFuture;
                    } else {
                        return CompletableFuture.failedFuture(
                                new AppException(AppError.CACHE_UNPROCESSABLE)
                        );
                    }
                });
    }
}
