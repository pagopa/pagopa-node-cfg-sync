package it.gov.pagopa.node.cfgsync.service;

import feign.FeignException;
import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronises the stand-in stations on the two independent databases (PagoPA PostgreSQL and
 * Nexi PostgreSQL).
 *
 * <p>Stand-in
 * synchronisation performs a <b>full table replace</b> ({@code deleteAll} + {@code saveAll}) on
 * each database. Because the databases live on different datasources and XA/JTA distributed
 * transactions are not available, atomicity ("all-or-nothing") is implemented at application level
 * through a <b>saga with compensating transactions</b> and a <b>fail-fast</b> strategy:
 * <ul>
 *   <li>each replace runs in its own local transaction and commits independently, and is retried
 *       up to {@code stand-in-manager.persistence.save-max-attempts} times on transient failures;</li>
 *   <li>the writes are executed in order and stop at the first definitive failure (fail-fast),
 *       so no further database is touched uselessly;</li>
 *   <li>before replacing a table the <b>previous content is captured</b> (snapshot); if any write
 *       fails, every target already replaced during this run is restored to its snapshot
 *       ({@code deleteAll} + {@code saveAll(snapshot)}), retried up to
 *       {@code stand-in-manager.persistence.rollback-max-attempts} times;</li>
 *   <li>if a compensation keeps failing the databases are left inconsistent and a critical alert
 *       is raised for manual reconciliation.</li>
 * </ul>
 */
@Service
@Slf4j
public class StandInManagerService extends CommonCacheService {

    private final boolean standInManagerEnabled;
    private final boolean standInManagerWritePagoPa;
    private final boolean standInManagerWriteNexiPostgres;
    /** Maximum number of attempts for a single table replace before giving up and triggering the rollback. */
    private final int saveMaxAttempts;
    /** Maximum number of attempts for a compensating snapshot restore before raising a critical alert. */
    private final int rollbackMaxAttempts;
    private final StandInManagerClient standInManagerClient;
    private final PagoPAStandInPostgresRepository pagoPAStandInPostgresRepository;
    private final NexiStandInPostgresRepository nexiStandInPostgresRepository;
    private final AppInsightTelemetryClient telemetryClient;
    private final TransactionTemplate pagopaPostgresTransactionTemplate;
    private final TransactionTemplate nexiPostgresTransactionTemplate;

    public StandInManagerService(
            @Value("${stand-in-manager.service.enabled}") boolean standInManagerEnabled,
            @Value("${stand-in-manager.write.pagopa-postgres}") boolean standInManagerWritePagoPa,
            @Value("${stand-in-manager.write.nexi-postgres}") boolean standInManagerWriteNexiPostgres,
            @Value("${stand-in-manager.persistence.save-max-attempts:3}") int saveMaxAttempts,
            @Value("${stand-in-manager.persistence.rollback-max-attempts:3}") int rollbackMaxAttempts,
            StandInManagerClient standInManagerClient,
            @Autowired(required = false) PagoPAStandInPostgresRepository pagoPAStandInPostgresRepository,
            @Autowired(required = false) NexiStandInPostgresRepository nexiStandInPostgresRepository,
            AppInsightTelemetryClient telemetryClient,
            @Autowired(required = false) @Qualifier("pagopaPostgresTransactionTemplate") TransactionTemplate pagopaPostgresTransactionTemplate,
            @Autowired(required = false) @Qualifier("nexiPostgresTransactionTemplate") TransactionTemplate nexiPostgresTransactionTemplate
    ) {
        this.standInManagerEnabled = standInManagerEnabled;
        this.standInManagerWritePagoPa = standInManagerWritePagoPa;
        this.standInManagerWriteNexiPostgres = standInManagerWriteNexiPostgres;
        this.saveMaxAttempts = saveMaxAttempts;
        this.rollbackMaxAttempts = rollbackMaxAttempts;
        this.standInManagerClient = standInManagerClient;
        this.pagoPAStandInPostgresRepository = pagoPAStandInPostgresRepository;
        this.nexiStandInPostgresRepository = nexiStandInPostgresRepository;
        this.telemetryClient = telemetryClient;
        this.pagopaPostgresTransactionTemplate = pagopaPostgresTransactionTemplate;
        this.nexiPostgresTransactionTemplate = nexiPostgresTransactionTemplate;
    }

    public Map<String, SyncStatusEnum> syncStandIn() {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();
        try {
            if (!standInManagerEnabled) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.standin.label);
            }

            StationsResponse stations = standInManagerClient.getStations();
            log.info("[{}] response successful. {} stations found", TargetRefreshEnum.standin.label, stations.getStations().size());
            List<StandInStations> stationsEntities = stations.getStations().stream().map(StandInStations::new).toList();

            List<StandInWriteStep> steps = List.of(
                    new StandInWriteStep(pagopaPostgresServiceIdentifier, standInManagerWritePagoPa,
                            pagopaPostgresTransactionTemplate, pagoPAStandInPostgresRepository),
                    new StandInWriteStep(nexiPostgresServiceIdentifier, standInManagerWriteNexiPostgres,
                            nexiPostgresTransactionTemplate, nexiStandInPostgresRepository)
            );

            return persistWithSaga(steps, stationsEntities, syncStatusMap);
        } catch (FeignException fEx) {
            log.error("[{}] error: {}", TargetRefreshEnum.standin.label, fEx.getMessage(), fEx);
            telemetryClient.createCustomEventForAlert(AppError.STANDIN_PROBLEM, "Feign exception", fEx);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, fEx);
        } catch (AppException appException) {
            telemetryClient.createCustomEventForAlert(AppError.STANDIN_PROBLEM, "App exception", appException);
            throw appException;
        } catch (Exception ex) {
            log.error("[{}][ALERT] Generic Error: {}", TargetRefreshEnum.standin.label, ex.getMessage(), ex);
            telemetryClient.createCustomEventForAlert(AppError.STANDIN_PROBLEM, "Generic exception", ex);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Executes the enabled replaces in order, each in its own local transaction, using a <b>fail-fast</b>
     * strategy: as soon as one replace definitively fails, no further database is touched and every
     * target already replaced during this run is restored to its previous snapshot.
     */
    private Map<String, SyncStatusEnum> persistWithSaga(List<StandInWriteStep> steps,
                                                        List<StandInStations> newStations,
                                                        Map<String, SyncStatusEnum> syncStatusMap) {
        // Targets whose table was really replaced in this run: only these must be rolled back.
        List<Compensation> compensations = new ArrayList<>();
        boolean anyError = false;

        for (StandInWriteStep step : steps) {
            if (!step.enabled()) {
                syncStatusMap.put(step.serviceId(), SyncStatusEnum.DISABLED);
            } else if (anyError) {
                // Fail-fast: a previous target already failed, so we skip this write entirely
                // (no useless DB operation). Nothing is persisted here, the run is rolled back.
                log.warn("[{}][ROLLBACK] Stand-in not written on {}: skipped after a previous failure (fail-fast)",
                        TargetRefreshEnum.standin.label, step.serviceId());
                syncStatusMap.put(step.serviceId(), SyncStatusEnum.ROLLBACK);
            } else {
                Compensation compensation = tryReplace(step, newStations);
                if (compensation != null) {
                    syncStatusMap.put(step.serviceId(), SyncStatusEnum.DONE);
                    compensations.add(compensation);
                } else {
                    syncStatusMap.put(step.serviceId(), SyncStatusEnum.ERROR);
                    anyError = true;
                }
            }
        }

        if (anyError) {
            compensate(compensations, syncStatusMap);
            log.error("[{}][ROLLBACK] Stand-in not applied atomically, compensation executed. Result: {}",
                    TargetRefreshEnum.standin.label, syncStatusMap);
        } else {
            log.info("[{}][ALERT-OK] Stand-in persisted on all targets. Result: {}",
                    TargetRefreshEnum.standin.label, syncStatusMap);
        }
        return syncStatusMap;
    }

    /**
     * Attempts a single full table replace, retrying up to {@code saveMaxAttempts} times on transient
     * failures. The previous content is captured before replacing it, so the target can be restored
     * on rollback. Returns the {@link Compensation} to run on rollback, or {@code null} if the replace
     * failed after all the allowed attempts.
     */
    private Compensation tryReplace(StandInWriteStep step, List<StandInStations> newStations) {
        for (int attempt = 1; attempt <= saveMaxAttempts; attempt++) {
            try {
                // Capture the current content BEFORE replacing it, to be able to restore it on rollback.
                List<StandInStations> previousSnapshot = new ArrayList<>(step.repository().findAll());
                step.transactionTemplate().executeWithoutResult(status -> {
                    step.repository().deleteAll();
                    step.repository().saveAll(newStations);
                });
                return new Compensation(step.serviceId(), step.transactionTemplate(), step.repository(), previousSnapshot);
            } catch (Exception ex) {
                log.error("[{}][ALERT] Save attempt {}/{} failed on {}: {}",
                        TargetRefreshEnum.standin.label, attempt, saveMaxAttempts, step.serviceId(), ex.getMessage(), ex);
                if (attempt == saveMaxAttempts) {
                    telemetryClient.createCustomEventForAlert(AppError.STANDIN_PROBLEM,
                            "Stand-in save failed on " + step.serviceId() + " after " + saveMaxAttempts + " attempts", ex);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Compensates every target replaced during this run by restoring its previous snapshot. Each
     * restore runs in its own local transaction and is retried up to {@code rollbackMaxAttempts}
     * times; if it keeps failing the databases are left inconsistent and a critical alert is raised
     * for manual reconciliation.
     */
    private void compensate(List<Compensation> compensations, Map<String, SyncStatusEnum> syncStatusMap) {
        for (Compensation compensation : compensations) {
            boolean rolledBack = false;
            for (int attempt = 1; attempt <= rollbackMaxAttempts && !rolledBack; attempt++) {
                try {
                    compensation.transactionTemplate().executeWithoutResult(status -> {
                        compensation.repository().deleteAll();
                        compensation.repository().saveAll(compensation.previousSnapshot());
                    });
                    rolledBack = true;
                    syncStatusMap.put(compensation.serviceId(), SyncStatusEnum.ROLLBACK);
                    log.warn("[{}][ROLLBACK] Restored previous stand-in snapshot ({} rows) on {} (attempt {}/{})",
                            TargetRefreshEnum.standin.label, compensation.previousSnapshot().size(),
                            compensation.serviceId(), attempt, rollbackMaxAttempts);
                } catch (Exception ex) {
                    log.error("[{}][ALERT] Rollback attempt {}/{} failed on {}: {}",
                            TargetRefreshEnum.standin.label, attempt, rollbackMaxAttempts,
                            compensation.serviceId(), ex.getMessage(), ex);
                }
            }
            if (!rolledBack) {
                // Worst case without XA: the replace is committed but cannot be undone -> real inconsistency.
                // Keep ERROR with a speaking message and raise a critical alert for manual reconciliation.
                syncStatusMap.put(compensation.serviceId(), SyncStatusEnum.ERROR);
                String detail = String.format(
                        "CRITICAL: rollback FAILED on %s after %d attempts. Database is INCONSISTENT, manual reconciliation required.",
                        compensation.serviceId(), rollbackMaxAttempts);
                log.error("[{}][CRITICAL] {}", TargetRefreshEnum.standin.label, detail);
                telemetryClient.createCustomEventForAlert(AppError.STANDIN_ROLLBACK_FAILED, detail, null);
            }
        }
    }

    /**
     * Descriptor of a single database target: the repository used to replace the stand-in stations.
     */
    private record StandInWriteStep(
            String serviceId,
            boolean enabled,
            TransactionTemplate transactionTemplate,
            JpaRepository<StandInStations, String> repository) {
    }

    /**
     * Compensation to execute on rollback: restores the previous snapshot captured before the replace.
     */
    private record Compensation(
            String serviceId,
            TransactionTemplate transactionTemplate,
            JpaRepository<StandInStations, String> repository,
            List<StandInStations> previousSnapshot) {
    }
}
