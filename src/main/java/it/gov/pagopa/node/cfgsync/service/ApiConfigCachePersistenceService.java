package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPACachePostgresRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_ID;
import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_TIMESTAMP;
import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_VERSION;

/**
 * Persists the configuration cache on the two independent databases (PagoPA PostgreSQL and
 * Nexi PostgreSQL).
 *
 * <p>Because the databases live on different datasources and XA/JTA distributed transactions are
 * not available, atomicity ("all-or-nothing") is implemented at application level through a
 * <b>saga with compensating transactions</b> and a <b>fail-fast</b> strategy:
 * <ul>
 *   <li>each insert runs in its own local transaction and commits independently, and is retried
 *       up to {@code api-config-cache.persistence.save-max-attempts} times on transient failures;</li>
 *   <li>the writes are executed in order and stop at the first definitive failure (fail-fast),
 *       so no further database is touched uselessly;</li>
 *   <li>every record actually inserted during this run is then compensated with a
 *       {@code deleteById} (on the same shared primary key), retried up to
 *       {@code api-config-cache.persistence.rollback-max-attempts} times;</li>
 *   <li>a duplicate-key error is treated as an idempotent success: the event is simply
 *       ignored because the cache is already present, and <b>no</b> database operation is
 *       performed (so it is never compensated, avoiding reconciliation problems).</li>
 * </ul>
 */
@Service
@Slf4j
public class ApiConfigCachePersistenceService extends CommonCacheService {

    private final boolean apiConfigCacheWritePagoPa;
    private final boolean apiConfigCacheWriteNexiPostgres;
    /** Maximum number of attempts for a single insert before giving up and triggering the rollback. */
    private final int saveMaxAttempts;
    /** Maximum number of attempts for a compensating delete before raising a critical alert. */
    private final int rollbackMaxAttempts;
    private final PagoPACachePostgresRepository pagoPACachePostgresRepository;
    private final NexiCachePostgresRepository nexiCachePostgresRepository;
    private final AppInsightTelemetryClient telemetryClient;
    private final TransactionTemplate pagopaPostgresTransactionTemplate;
    private final TransactionTemplate nexiPostgresTransactionTemplate;

    public ApiConfigCachePersistenceService(
            @Value("${api-config-cache.write.pagopa-postgres}") boolean apiConfigCacheWritePagoPa,
            @Value("${api-config-cache.write.nexi-postgres}") boolean apiConfigCacheWriteNexiPostgres,
            @Value("${api-config-cache.persistence.save-max-attempts:3}") int saveMaxAttempts,
            @Value("${api-config-cache.persistence.rollback-max-attempts:3}") int rollbackMaxAttempts,
            @Autowired(required = false) PagoPACachePostgresRepository pagoPACachePostgresRepository,
            @Autowired(required = false) NexiCachePostgresRepository nexiCachePostgresRepository,
            AppInsightTelemetryClient telemetryClient,
            @Autowired(required = false) @Qualifier("pagopaPostgresTransactionTemplate") TransactionTemplate pagopaPostgresTransactionTemplate,
            @Autowired(required = false) @Qualifier("nexiPostgresTransactionTemplate") TransactionTemplate nexiPostgresTransactionTemplate
    ) {
        this.apiConfigCacheWritePagoPa = apiConfigCacheWritePagoPa;
        this.apiConfigCacheWriteNexiPostgres = apiConfigCacheWriteNexiPostgres;
        this.saveMaxAttempts = saveMaxAttempts;
        this.rollbackMaxAttempts = rollbackMaxAttempts;
        this.pagoPACachePostgresRepository = pagoPACachePostgresRepository;
        this.nexiCachePostgresRepository = nexiCachePostgresRepository;
        this.telemetryClient = telemetryClient;
        this.pagopaPostgresTransactionTemplate = pagopaPostgresTransactionTemplate;
        this.nexiPostgresTransactionTemplate = nexiPostgresTransactionTemplate;
    }

    public Map<String, SyncStatusEnum> saveCache(ResponseEntity<byte[]> response) {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();

        try {
            HttpHeaders headers = response.getHeaders();
            if (headers.isEmpty()) {
                throw new AppException(AppError.INTERNAL_SERVER_ERROR, "empty headers");
            }

            String cacheId = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_ID);
            String cacheTimestamp = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_TIMESTAMP);
            String cacheVersion = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_VERSION);

            ConfigCache configCache = composeCache(
                    cacheId,
                    ZonedDateTime.parse(cacheTimestamp),
                    cacheVersion,
                    response.getBody()
            );

            List<CacheWriteStep> steps = List.of(
                    new CacheWriteStep(pagopaPostgresServiceIdentifier, apiConfigCacheWritePagoPa, pagopaPostgresTransactionTemplate,
                            pagoPACachePostgresRepository == null ? null : pagoPACachePostgresRepository::save,
                            pagoPACachePostgresRepository == null ? null : pagoPACachePostgresRepository::deleteById),
                    new CacheWriteStep(nexiPostgresServiceIdentifier, apiConfigCacheWriteNexiPostgres, nexiPostgresTransactionTemplate,
                            nexiCachePostgresRepository == null ? null : nexiCachePostgresRepository::save,
                            nexiCachePostgresRepository == null ? null : nexiCachePostgresRepository::deleteById)
            );

            return persistWithSaga(cacheId, configCache, steps, syncStatusMap);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Executes the enabled writes in order, each in its own local transaction, using a <b>fail-fast</b>
     * strategy: as soon as one insert definitively fails, no further database is touched and every
     * record already inserted during this run is compensated.
     */
    private Map<String, SyncStatusEnum> persistWithSaga(String cacheId,
                                                        ConfigCache configCache,
                                                        List<CacheWriteStep> steps,
                                                        Map<String, SyncStatusEnum> syncStatusMap) {
        // Steps whose insert really hit the database in this run: only these must be rolled back.
        List<CacheWriteStep> compensatableSteps = new ArrayList<>();
        boolean anyError = false;

        for (CacheWriteStep step : steps) {
            if (!step.enabled()) {
                syncStatusMap.put(step.serviceId(), SyncStatusEnum.DISABLED);
            } else if (anyError) {
                // Fail-fast: a previous target already failed, so we skip this write entirely
                // (no useless DB operation). Nothing is persisted here, the run is rolled back.
                log.warn("[{}][ROLLBACK] Cache id={} not written on {}: skipped after a previous failure (fail-fast)",
                        TargetRefreshEnum.cache.label, cacheId, step.serviceId());
                syncStatusMap.put(step.serviceId(), SyncStatusEnum.ROLLBACK);
            } else {
                switch (trySave(step, configCache, cacheId)) {
                    case INSERTED -> {
                        syncStatusMap.put(step.serviceId(), SyncStatusEnum.DONE);
                        compensatableSteps.add(step);
                    }
                    case ALREADY_PRESENT -> syncStatusMap.put(step.serviceId(), SyncStatusEnum.DONE);
                    case FAILED -> {
                        syncStatusMap.put(step.serviceId(), SyncStatusEnum.ERROR);
                        anyError = true;
                    }
                }
            }
        }

        if (anyError) {
            compensate(cacheId, compensatableSteps, syncStatusMap);
            log.error("[{}][ROLLBACK] Cache id={} not applied atomically, compensation executed. Result: {}",
                    TargetRefreshEnum.cache.label, cacheId, syncStatusMap);
        } else {
            log.info("[{}][ALERT-OK] Cache id={} persisted on all targets. Result: {}",
                    TargetRefreshEnum.cache.label, cacheId, syncStatusMap);
        }
        return syncStatusMap;
    }

    /**
     * Attempts a single insert, retrying up to {@code saveMaxAttempts} times on transient failures.
     * A duplicate-key error is NOT retried: it means the cache is already present, so the event is
     * treated as an idempotent success without touching the database.
     */
    private SaveOutcome trySave(CacheWriteStep step, ConfigCache configCache, String cacheId) {
        for (int attempt = 1; attempt <= saveMaxAttempts; attempt++) {
            try {
                step.transactionTemplate().executeWithoutResult(status -> step.insert().accept(configCache));
                return SaveOutcome.INSERTED;
            } catch (DataIntegrityViolationException duplicate) {
                // Idempotent case: the cache id is already present on this database (event already
                // applied by a previous run). We keep it as a success and intentionally perform NO
                // database operation, so this record will never be compensated.
                log.warn("[{}][IDEMPOTENT-SKIP] Cache id={} already present on {}: event ignored, "
                                + "no write performed to avoid reconciliation issues. Cause: {}",
                        TargetRefreshEnum.cache.label, cacheId, step.serviceId(), duplicate.getMessage());
                return SaveOutcome.ALREADY_PRESENT;
            } catch (Exception ex) {
                log.error("[{}][ALERT] Save attempt {}/{} failed for cache id={} on {}: {}",
                        TargetRefreshEnum.cache.label, attempt, saveMaxAttempts, cacheId, step.serviceId(), ex.getMessage(), ex);
                if (attempt == saveMaxAttempts) {
                    telemetryClient.createCustomEventForAlert(AppError.CACHE_SAVE_PROBLEM,
                            "Cache save failed on " + step.serviceId() + " for cache id=" + cacheId + " after " + saveMaxAttempts + " attempts", ex);
                    return SaveOutcome.FAILED;
                }
            }
        }
        return SaveOutcome.FAILED;
    }

    /**
     * Compensates (deletes) every record inserted during this run. Each delete runs in its own
     * local transaction and is retried up to {@code rollbackMaxAttempts} times; if it keeps failing
     * the databases are left inconsistent and a critical alert is raised for manual reconciliation.
     */
    private void compensate(String cacheId,
                            List<CacheWriteStep> compensatableSteps,
                            Map<String, SyncStatusEnum> syncStatusMap) {
        for (CacheWriteStep step : compensatableSteps) {
            boolean rolledBack = false;
            for (int attempt = 1; attempt <= rollbackMaxAttempts && !rolledBack; attempt++) {
                try {
                    step.transactionTemplate().executeWithoutResult(status -> step.deleteById().accept(cacheId));
                    rolledBack = true;
                    syncStatusMap.put(step.serviceId(), SyncStatusEnum.ROLLBACK);
                    log.warn("[{}][ROLLBACK] Compensated cache id={} on {} (attempt {}/{})",
                            TargetRefreshEnum.cache.label, cacheId, step.serviceId(), attempt, rollbackMaxAttempts);
                } catch (Exception ex) {
                    log.error("[{}][ALERT] Rollback attempt {}/{} failed for cache id={} on {}: {}",
                            TargetRefreshEnum.cache.label, attempt, rollbackMaxAttempts, cacheId,
                            step.serviceId(), ex.getMessage(), ex);
                }
            }
            if (!rolledBack) {
                // Worst case without XA: the write is committed but cannot be undone -> real inconsistency.
                // Keep ERROR with a speaking message and raise a critical alert for manual reconciliation.
                syncStatusMap.put(step.serviceId(), SyncStatusEnum.ERROR);
                String detail = String.format(
                        "CRITICAL: rollback FAILED on %s for cache id=%s after %d attempts. Database is INCONSISTENT, manual reconciliation required.",
                        step.serviceId(), cacheId, rollbackMaxAttempts);
                log.error("[{}][CRITICAL] {}", TargetRefreshEnum.cache.label, detail);
                telemetryClient.createCustomEventForAlert(AppError.CACHE_ROLLBACK_FAILED, detail, null);
            }
        }
    }

    /** Outcome of a single (retried) insert attempt on a target database. */
    private enum SaveOutcome {
        /** The record was newly inserted (compensatable on rollback). */
        INSERTED,
        /** The record was already present (idempotent, no DB operation performed, not compensatable). */
        ALREADY_PRESENT,
        /** The insert failed after all the allowed attempts. */
        FAILED
    }

    /**
     * Descriptor of a single database target: how to insert the cache and how to compensate it.
     */
    private record CacheWriteStep(
            String serviceId,
            boolean enabled,
            TransactionTemplate transactionTemplate,
            Consumer<ConfigCache> insert,
            Consumer<String> deleteById) {
    }
}
