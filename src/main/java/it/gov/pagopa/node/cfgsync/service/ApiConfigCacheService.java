package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
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
    private static final String CACHE_UNPROCESSABLE_DETAILS = "Cache not ready to be saved after retries";

    @Value("${api-config-cache.service.enabled}")
    private boolean apiConfigCacheServiceEnabled;

    private final ApiConfigCacheFetchService fetchService;
    private final ApiConfigCachePersistenceService persistenceService;
    private final AppInsightTelemetryClient telemetryClient;

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
        } catch (InterruptedException e) {
            telemetryClient.createCustomEventForAlert(AppError.CACHE_UNPROCESSABLE, CACHE_UNPROCESSABLE_DETAILS, e);
            Thread.currentThread().interrupt();
            throw new AppException(AppError.CACHE_UNPROCESSABLE, e, CACHE_UNPROCESSABLE_DETAILS);
        } catch (ExecutionException e) {
            telemetryClient.createCustomEventForAlert(AppError.CACHE_UNPROCESSABLE, CACHE_UNPROCESSABLE_DETAILS, e);
            Throwable cause = e.getCause();
            if (cause instanceof AppException appException) {
                // propagate the original error (fetch or save) without masking its status/title
                throw appException;
            }
            throw new AppException(AppError.CACHE_UNPROCESSABLE, cause != null ? cause : e, CACHE_UNPROCESSABLE_DETAILS);
        }
    }

    public void customEvent() {
        telemetryClient.createCustomEventForAlert(AppError.FAKE, "Test details " + LocalDateTime.now(), null);
    }
}
