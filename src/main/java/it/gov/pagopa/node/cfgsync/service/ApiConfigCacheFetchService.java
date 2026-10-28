package it.gov.pagopa.node.cfgsync.service;

import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ApiConfigCacheFetchService {

    private final ApiConfigCacheClient apiConfigCacheClient;

    public ApiConfigCacheFetchService(ApiConfigCacheClient apiConfigCacheClient) {
        this.apiConfigCacheClient = apiConfigCacheClient;
    }

    public CompletableFuture<Response> fetchCacheWithRetry() {
        return CompletableFuture.supplyAsync(apiConfigCacheClient::getCache)
                .handle((response, ex) -> {
                    if (response != null && response.status() == HttpStatus.OK.value()) {
                        return CompletableFuture.completedFuture(response);
                    }
                    log.error("Fetching cache returned non-OK status={} and will not be retried", response != null ? response.status() : null, ex);
                    return CompletableFuture.failedFuture(new AppException(AppError.CACHE_UNPROCESSABLE));
                })
                .thenCompose(future -> (CompletableFuture<Response>) future);
    }
}
