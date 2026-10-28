package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ApiConfigCacheFetchService {

    private final ApiConfigCacheClient apiConfigCacheClient;

    public ApiConfigCacheFetchService(ApiConfigCacheClient apiConfigCacheClient) {
        this.apiConfigCacheClient = apiConfigCacheClient;
    }

    public CompletableFuture<ResponseEntity<byte[]>> fetchCacheWithRetry() {
        return CompletableFuture.supplyAsync(apiConfigCacheClient::getCache)
                .handle((response, ex) -> {
                    if (ex == null && response != null && response.getStatusCode().is2xxSuccessful()) {
                        return CompletableFuture.completedFuture(response);
                    }
                    log.error("Fetching cache failed after retries", ex);
                    return CompletableFuture.<ResponseEntity<byte[]>>failedFuture(new AppException(AppError.CACHE_UNPROCESSABLE));
                })
                .thenCompose(future -> future);
    }
}
