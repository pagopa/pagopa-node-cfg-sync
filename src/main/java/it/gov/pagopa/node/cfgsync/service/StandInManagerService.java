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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StandInManagerService extends CommonCacheService {

    @Value("${stand-in-manager.service.enabled}")
    private boolean enabled;
    @Value("${stand-in-manager.service.subscriptionKey}")
    private String subscriptionKey;
    private final StandInManagerClient standInManagerClient;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private PagoPAStandInPostgresRepository pagopaPostgresRepository;
    @Autowired(required = false)
    private NexiStandInPostgresRepository nexiPostgresRepository;
    @Autowired(required = false)
    private NexiStandInOracleRepository nexiOracleRepository;

//    @Value("${app.write.standin.pagopa-postgres}")
//    private Boolean pagopaPostgresWrite;
    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgresServiceIdentifier;

//    @Value("${app.write.standin.nexi-postgres}")
//    private Boolean nexiPostgresWrite;
    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgresServiceIdentifier;

//    @Value("${app.write.standin.nexi-oracle}")
//    private Boolean nexiOracleWrite;
    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    public StandInManagerService(@Value("${stand-in-manager.service.host}") String standInManagerUrl, ObjectMapper objectMapper) {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
        this.objectMapper = objectMapper;
    }

    public Map<String, SyncStatusEnum> forceStandIn() {
        Map<String, SyncStatusEnum> syncStatusMap = new HashMap<>();
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.standin);
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

            saveAllDatabases(syncStatusMap, stationsEntities);
        } catch (SyncDbStatusException e) {
            //viene usata per poter restituire in risposta la mappa degli aggiornamenti
            return syncStatusMap;
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService stand-in-manager get stations error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("SyncService stand-in-manager get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return syncStatusMap;
    }

    @Transactional(rollbackFor={SyncDbStatusException.class})
    void saveAllDatabases(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) throws SyncDbStatusException {
        savePagoPA(syncStatusMap, stationsEntities);
        saveNexiPostgres(syncStatusMap, stationsEntities);
        saveNexiOracle(syncStatusMap, stationsEntities);

        if( syncStatusMap.containsValue(SyncStatusEnum.error) ) {
            syncStatusMap.forEach((k, v) -> {
                if (v == SyncStatusEnum.done) {
                    syncStatusMap.remove(k);
                    syncStatusMap.put(k, SyncStatusEnum.rollback);
                }
            });
            throw new SyncDbStatusException("Rollback sync");
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if( null != pagopaPostgresRepository ) {
                pagopaPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.done);
            } else {
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.disabled);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save pagoPA error: {}", ex.getMessage(), ex);
            syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.error);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if( null != nexiOracleRepository ) {
                nexiOracleRepository.saveAll(stationsEntities);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.done);
            } else {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.disabled);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save Nexi Oracle error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.error);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if ( null != nexiPostgresRepository ) {
                nexiPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.done);
            } else {
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.disabled);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save Nexi Postgres error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.error);
        }
    }
}
