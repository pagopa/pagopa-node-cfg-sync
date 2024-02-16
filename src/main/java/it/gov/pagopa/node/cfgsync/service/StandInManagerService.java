package it.gov.pagopa.node.cfgsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiStandInOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgre.NexiStandInPostgreRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private Optional<PagoPAStandInPostgreRepository> pagoPAStandInPostgreRepository;
    @Autowired(required = false)
    private Optional<NexiStandInPostgreRepository> nexiStandInPostgreRepository;
    @Autowired(required = false)
    private Optional<NexiStandInOracleRepository> nexiStandInOracleRepository;

    @Value("${app.write.standin.pagopa-postgres}")
    private Boolean pagopaPostgreStandInEnabled;
    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgreServiceIdentifier;

    @Value("${app.write.standin.nexi-postgres}")
    private Boolean nexiPostgreStandInEnabled;
    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgreServiceIdentifier;

    @Value("${app.write.standin.nexi-oracle}")
    private Boolean nexiOracleStandInEnabled;
    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    public StandInManagerService(@Value("${stand-in-manager.service.host}") String standInManagerUrl, ObjectMapper objectMapper) {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
        this.objectMapper = objectMapper;
    }

    @Transactional
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
            List<StandInStations> stationsEntities = stations.getStations().stream().map(StandInStations::new).collect(Collectors.toList());

            try {
                if ( pagopaPostgreStandInEnabled && pagoPAStandInPostgreRepository.isPresent() ) {
                    pagoPAStandInPostgreRepository.get().deleteAll();
                    pagoPAStandInPostgreRepository.get().saveAll(stationsEntities);
                    syncStatusMap.put(pagopaPostgreServiceIdentifier, SyncStatusEnum.done);
                } else {
                    syncStatusMap.put(pagopaPostgreServiceIdentifier, SyncStatusEnum.disabled);
                }
            } catch(Exception ex) {
                syncStatusMap.put(pagopaPostgreServiceIdentifier, SyncStatusEnum.error);
            }
            try {
                if ( nexiPostgreStandInEnabled && nexiStandInPostgreRepository.isPresent() ) {
                    nexiStandInPostgreRepository.get().deleteAll();
                    nexiStandInPostgreRepository.get().saveAll(stationsEntities);
                    syncStatusMap.put(nexiPostgreServiceIdentifier, SyncStatusEnum.done);
                } else {
                    syncStatusMap.put(nexiPostgreServiceIdentifier, SyncStatusEnum.disabled);
                }
            } catch(Exception ex) {
                syncStatusMap.put(nexiPostgreServiceIdentifier, SyncStatusEnum.error);
            }
            try {
                if( nexiOracleStandInEnabled && nexiStandInOracleRepository.isPresent() ) {
                    nexiStandInOracleRepository.get().deleteAll();
                    nexiStandInOracleRepository.get().saveAll(stationsEntities);
                    syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.done);
                } else {
                    syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.disabled);
                }
            } catch(Exception ex) {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.error);
            }
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService stand-in-manager get stations error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (FeignException | IOException e) {
            log.error("SyncService stand-in-manager get stations error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return syncStatusMap;
    }
}
