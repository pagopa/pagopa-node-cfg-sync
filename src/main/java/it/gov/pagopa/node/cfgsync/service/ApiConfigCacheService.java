package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Setter
@Slf4j
@RequiredArgsConstructor
public class ApiConfigCacheService {
    @Value("${api-config-cache.service.enabled}")
    private boolean apiConfigCacheServiceEnabled;

    private final ApiConfigCacheFetchService fetchService;
    private final ApiConfigCachePersistenceService persistenceService;

    public Map<String, SyncStatusEnum> syncCache() {
        if( !apiConfigCacheServiceEnabled) {
            throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.cache.label);
        }

        log.info("Starting sync cache [{}]", LocalDateTime.now());
        try {
            Map<String, SyncStatusEnum> response = fetchService.fetchCacheWithRetry()
                    .thenApply(persistenceService::saveCache).get();
            log.info("Done sync cache [{}]", LocalDateTime.now());
            return response;
        } catch (ExecutionException | InterruptedException e) {
            throw new AppException(AppError.CACHE_UNPROCESSABLE, "Cache not ready to be saved after retries");
        }
    }
}
