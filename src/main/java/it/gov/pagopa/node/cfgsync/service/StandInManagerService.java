package it.gov.pagopa.node.cfgsync.service;

import feign.FeignException;
import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiStandInOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StandInManagerService extends CommonCacheService {

    private final boolean standInManagerEnabled;
    private final boolean standInManagerWritePagoPa;
    private final boolean standInManagerWriteNexiOracle;
    private final boolean standInManagerWriteNexiPostgres;
    private final StandInManagerClient standInManagerClient;
    private final PagoPAStandInPostgresRepository pagoPAStandInPostgresRepository;
    private final NexiStandInPostgresRepository nexiStandInPostgresRepository;
    private final NexiStandInOracleRepository nexiStandInOracleRepository;
    private final AppInsightTelemetryClient telemetryClient;

    public StandInManagerService(
            @Value("${stand-in-manager.service.enabled}") boolean standInManagerEnabled,
            @Value("${stand-in-manager.write.pagopa-postgres}") boolean standInManagerWritePagoPa,
            @Value("${stand-in-manager.write.nexi-oracle}") boolean standInManagerWriteNexiOracle,
            @Value("${stand-in-manager.write.nexi-postgres}") boolean standInManagerWriteNexiPostgres,
            StandInManagerClient standInManagerClient,
             @Autowired(required = false) PagoPAStandInPostgresRepository pagoPAStandInPostgresRepository,
             @Autowired(required = false) NexiStandInPostgresRepository nexiStandInPostgresRepository,
             @Autowired(required = false) NexiStandInOracleRepository nexiStandInOracleRepository,
            AppInsightTelemetryClient telemetryClient
    ) {
        this.standInManagerEnabled = standInManagerEnabled;
        this.standInManagerWritePagoPa = standInManagerWritePagoPa;
        this.standInManagerWriteNexiOracle = standInManagerWriteNexiOracle;
        this.standInManagerWriteNexiPostgres = standInManagerWriteNexiPostgres;
        this.standInManagerClient = standInManagerClient;
        this.pagoPAStandInPostgresRepository = pagoPAStandInPostgresRepository;
        this.nexiStandInPostgresRepository = nexiStandInPostgresRepository;
        this.nexiStandInOracleRepository = nexiStandInOracleRepository;
        this.telemetryClient = telemetryClient;
    }

    @Transactional(rollbackFor = {SyncDbStatusException.class})
    public Map<String, SyncStatusEnum> syncStandIn() {
        if (!standInManagerEnabled) {
            AppException appException = new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.standin.label);
            telemetryClient.createCustomEventForAlert(AppError.SERVICE_DISABLED, "App exception", appException);
            throw appException;
        }

        StationsResponse stations = getStations();
        try {
            Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();
            List<StandInStations> stationsEntities = stations.getStations().stream().map(StandInStations::new).toList();

            savePagoPA(syncStatusMap, stationsEntities);
            saveNexiPostgres(syncStatusMap, stationsEntities);
            saveNexiOracle(syncStatusMap, stationsEntities);

            return composeSyncStatusMapResult(TargetRefreshEnum.standin.label, syncStatusMap);
        } catch (Exception ex) {
            log.error("[{}][ALERT] Generic Error: {}", TargetRefreshEnum.standin.label, ex.getMessage(), ex);
            telemetryClient.createCustomEventForAlert(AppError.STANDIN_PROBLEM, "Generic exception", ex);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }

    private @NonNull StationsResponse getStations() {
        StationsResponse stations;
        try {
            stations = standInManagerClient.getStations();
        } catch (FeignException e) {
            log.error("[{}] error: {}", TargetRefreshEnum.standin.label, e.getMessage(), e);
            telemetryClient.createCustomEventForAlert(AppError.STANDIN_PROBLEM, "Feign exception", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        log.info("[{}] response successful. {} stations found", TargetRefreshEnum.standin.label, stations.getStations().size());
        return stations;
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if (standInManagerWritePagoPa) {
                pagoPAStandInPostgresRepository.deleteAll();
                pagoPAStandInPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch (Exception ex) {
            log.error("[{}][ALERT] Problem to dump cache on PagoPA PostgreSQL: {}", TargetRefreshEnum.standin.label, ex.getMessage(), ex);
            syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if (standInManagerWriteNexiOracle) {
                nexiStandInOracleRepository.deleteAll();
                nexiStandInOracleRepository.saveAll(stationsEntities);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch (Exception ex) {
            log.error("[{}][ALERT] Problem to dump cache on Nexi Oracle: {}", TargetRefreshEnum.standin.label, ex.getMessage(), ex);
            syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if (standInManagerWriteNexiPostgres) {
                nexiStandInPostgresRepository.deleteAll();
                nexiStandInPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch (Exception ex) {
            log.error("[{}][ALERT] Problem to dump cache on Nexi PostgreSQL: {}", TargetRefreshEnum.standin.label, ex.getMessage(), ex);
            syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }
}
