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
    private boolean standInManagerEnabled;

    @Value("${stand-in-manager.service.subscriptionKey}")
    private String standInManagerSubscriptionKey;

    @Value("${stand-in-manager.service.host}")
    private String standInManagerUrl;

    @Value("${stand-in-manager.write.pagopa-postgres}")
    private boolean standInManagerWritePagoPa;

    @Value("${stand-in-manager.write.nexi-oracle}")
    private boolean standInManagerWriteNexiOracle;

    @Value("${stand-in-manager.write.nexi-postgres}")
    private boolean standInManagerWriteNexiPostgres;

    private StandInManagerClient standInManagerClient;

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private PagoPAStandInPostgresRepository pagoPAStandInPostgresRepository;
    @Autowired(required = false)
    private NexiStandInPostgresRepository nexiStandInPostgresRepository;
    @Autowired(required = false)
    private NexiStandInOracleRepository nexiStandInOracleRepository;

    @PostMapping
    private void setStandInManagerClient() {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
    }

    @Transactional(rollbackFor={SyncDbStatusException.class})
    public Map<String, SyncStatusEnum> syncStandIn() {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();
        try {
            if( !standInManagerEnabled) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.standin.label);
            }
            log.debug("SyncService stand-in-manager get stations");
            Response response = standInManagerClient.getCache(standInManagerSubscriptionKey);
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

            return composeSyncStatusMapResult(syncStatusMap);
        } catch (FeignException e) {
            log.error("SyncService stand-in-manager get cache error: {}", e.getMessage(), e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch(AppException appException) {
            throw appException;
        } catch (Exception e) {
            log.error("SyncService stand-in-manager get cache error: {}", e.getMessage(), e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if(standInManagerWritePagoPa) {
                pagoPAStandInPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(getPagopaPostgresServiceIdentifier(), SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(getPagopaPostgresServiceIdentifier(), SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save pagoPA error: {}", ex.getMessage(), ex);
            syncStatusMap.put(getPagopaPostgresServiceIdentifier(), SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if(standInManagerWriteNexiOracle) {
                nexiStandInOracleRepository.saveAll(stationsEntities);
                syncStatusMap.put(getNexiOracleServiceIdentifier(), SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(getNexiOracleServiceIdentifier(), SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save Nexi Oracle error: {}", ex.getMessage(), ex);
            syncStatusMap.put(getNexiOracleServiceIdentifier(), SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, List<StandInStations> stationsEntities) {
        try {
            if (standInManagerWriteNexiPostgres) {
                nexiStandInPostgresRepository.saveAll(stationsEntities);
                syncStatusMap.put(getNexiPostgresServiceIdentifier(), SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(getNexiPostgresServiceIdentifier(), SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService stand-in-manager save Nexi Postgres error: {}", ex.getMessage(), ex);
            syncStatusMap.put(getNexiPostgresServiceIdentifier(), SyncStatusEnum.ERROR);
        }
    }
}
