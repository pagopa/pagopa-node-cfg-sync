package it.gov.pagopa.node.cfgsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.Response;
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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Setter
@Slf4j
@RequiredArgsConstructor
public class StandInManagerService extends CommonCacheService {

    @Value("${stand-in-manager.service.enabled}")
    private boolean enabled;

    @Value("${stand-in-manager.service.subscriptionKey}")
    private String subscriptionKey;

    @Value("${stand-in-manager.service.host}")
    private String standInManagerUrl;

    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    @Value("${stand-in-manager.write.pagopa-postgres}")
    private boolean writePagoPa;

    @Value("${stand-in-manager.write.nexi-oracle}")
    private boolean writeNexiOracle;

    @Value("${stand-in-manager.write.nexi-postgres}")
    private boolean writeNexiPostgres;

    private StandInManagerClient standInManagerClient;

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private PagoPAStandInPostgresRepository pagopaPostgresRepository;
    @Autowired(required = false)
    private NexiStandInPostgresRepository nexiPostgresRepository;
    @Autowired(required = false)
    private NexiStandInOracleRepository nexiOracleRepository;

    @PostMapping
    private void setStandInManagerClient() {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
    }

    @Transactional(rollbackFor={SyncDbStatusException.class})
    public Map<String, SyncStatusEnum> forceStandIn() {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.standin.label);
            }
            log.debug("SyncService stand-in-manager get stations");
            Response response = standInManagerClient.getCache(subscriptionKey);
            int httpResponseCode = response.status();
            if (httpResponseCode != HttpStatus.OK.value()) {
                log.error("SyncService stand-in-manager get stations error - result: httpStatusCode[{}]", httpResponseCode);
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }
            log.info("SyncService stand-in-manager get stations successful");

            StationsResponse stations = objectMapper.readValue(response.body().asInputStream().readAllBytes(), StationsResponse.class);
            log.info("SyncService {} stations found", stations.getStations().size());
            List<StandInStations> stationsEntities = stations.getStations().stream().map(StandInStations::new).toList();

            savePagoPA(syncStatusMap, stationsEntities);
            saveNexiPostgres(syncStatusMap, stationsEntities);
            saveNexiOracle(syncStatusMap, stationsEntities);

            Map<String, SyncStatusEnum> syncStatusMapUpdated = new LinkedHashMap<>();
            if( syncStatusMap.containsValue(SyncStatusEnum.ERROR) ) {
                syncStatusMap.forEach((k, v) -> {
                    if (v == SyncStatusEnum.DONE) {
                        syncStatusMapUpdated.put(k, SyncStatusEnum.ROLLBACK);
                    } else {
                        syncStatusMapUpdated.put(k, v);
                    }
                });
                return syncStatusMapUpdated;
            } else {
                return syncStatusMap;
            }
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService stand-in-manager get stations error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch(AppException appException) {
            throw appException;
        } catch (Exception e) {
            log.error("SyncService stand-in-manager get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if( writePagoPa ) {
                pagopaPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save pagoPA error: {}", ex.getMessage(), ex);
            syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if( writeNexiOracle ) {
                nexiOracleRepository.saveAll(stationsEntities);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save Nexi Oracle error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if ( writeNexiPostgres ) {
                nexiPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save Nexi Postgres error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }
}
