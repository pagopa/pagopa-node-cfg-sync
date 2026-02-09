package it.gov.pagopa.node.cfgsync.service;

import feign.Feign;
import feign.FeignException;
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
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
        log.info("Fetching cache with {} attempt(s) in {} sec", attempt, Math.pow(2, attempt-1d));

        return CompletableFuture.supplyAsync(() -> apiConfigCacheClient.getCache(apiConfigCacheSubscriptionKey))
                .handle((response, ex) -> {
                    // unwrap CompletionException to get the real cause
                    Throwable cause = ex;
                    if (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }

                    boolean shouldRetry = true;

                    // if we have a successful response and status OK, return completed future
                    if (cause == null) {
                        if (response != null && response.status() == HttpStatus.OK.value()) {
                            return CompletableFuture.completedFuture(response);
                        }
                        // cause is null but response is null or non-OK -> consider retry
                        // no exception, but fall through to retry logic below
                        // determine if we should retry: already set to true for non-OK/null response
                        if (retryLeft > 0) {
                            long delay = (long) Math.pow(2, attempt);
                            CompletableFuture<Response> retryFuture = new CompletableFuture<>();
                            log.warn("Fetch failed (response={} cause={}). Scheduling retry in {}s ({} retries left)",
                                    response != null ? response.status() : null,
                                    "no-exception",
                                    delay,
                                    retryLeft - 1);

                            scheduler.schedule(
                                    () -> fetchWithRetry(retryLeft - 1, attempt + 1)
                                            .whenComplete((res, exc) -> {
                                                if (exc != null) retryFuture.completeExceptionally(exc);
                                                else retryFuture.complete(res);
                                            }),
                                    delay, TimeUnit.SECONDS
                            );
                            return retryFuture;
                        } else {
                            log.error("Fetching cache returned non-OK status={} and will not be retried", response != null ? response.status() : null);
                            return CompletableFuture.failedFuture(new AppException(AppError.CACHE_UNPROCESSABLE));
                        }
                    }
                    else {
                        // determine if we should retry: exception-based logic
                        shouldRetry = false;
                        // retry for feign.RetryableException and IO transient errors
                        if (cause instanceof feign.RetryableException) {
                            shouldRetry = true;
                        } else if (cause instanceof IOException) {
                            shouldRetry = true;
                        } else if (cause instanceof FeignException) {
                            try {
                                int status = ((FeignException) cause).status();
                                if (status >= 404) {
                                    shouldRetry = true;
                                }
                            } catch (Exception ignore) {
                                // if we can't read status, don't retry by default
                            }
                        }
                    }

                    if (shouldRetry && retryLeft > 0) {
                        long delay = (long) Math.pow(2, attempt);
                        CompletableFuture<Response> retryFuture = new CompletableFuture<>();
                        log.warn("Fetch failed (response={} cause={}). Scheduling retry in {}s ({} retries left)",
                                response != null ? response.status() : null,
                                cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : "no-exception",
                                delay,
                                retryLeft - 1);

                        scheduler.schedule(
                                () -> fetchWithRetry(retryLeft - 1, attempt + 1)
                                        .whenComplete((res, exc) -> {
                                            if (exc != null) retryFuture.completeExceptionally(exc);
                                            else retryFuture.complete(res);
                                        }),
                                delay, TimeUnit.SECONDS
                        );
                        return retryFuture;
                    } else {
                        // No retries left or not retryable -> fail with AppException
                        if (cause != null) {
                            log.error("Fetching cache failed and will not be retried (cause={})", cause.toString());
                        } else {
                            log.error("Fetching cache returned non-OK status={} and will not be retried", response != null ? response.status() : null);
                        }
                        return CompletableFuture.failedFuture(new AppException(AppError.CACHE_UNPROCESSABLE));
                    }
                })
                .thenCompose(future -> (CompletableFuture<Response>) future);
    }
}
