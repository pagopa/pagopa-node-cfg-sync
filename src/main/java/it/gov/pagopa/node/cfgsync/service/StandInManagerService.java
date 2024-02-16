package it.gov.pagopa.node.cfgsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.standin.NexiStandInOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgre.standin.NexiStandInPostgreRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.standin.PagoPAStandInPostgreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class StandInManagerService extends CommonCacheService {

    @Value("${stand-in-manager.service.enabled}")
    private boolean enabled;
    @Value("${stand-in-manager.service.subscriptionKey}")
    private String subscriptionKey;
    @Value("${stand-in-manager.rx-connection-string}")
    private String standInRxConnectionString;
    @Value("${stand-in-manager.rx-name}")
    private String standInRxName;
    @Value("${stand-in-manager.sa-connection-string}")
    private String standInSaConnectionString;
    @Value("${stand-in-manager.sa-name}")
    private String standInSaContainerName;
    @Value("${stand-in-manager.consumer-group}")
    private String standInConsumerGroup;

    private final StandInManagerClient standInManagerClient;
    private final ObjectMapper objectMapper;

    @Autowired
    private PagoPAStandInPostgreRepository pagoPAStandInPostgreRepository;
//    @Autowired
//    private NexiStandInPostgreRepository nexiStandInPostgreRepository;
//    @Autowired
//    private NexiStandInOracleRepository nexiStandInOracleRepository;

    @Value("${spring.datasource.pagopa.postgre.standin.enabled}")
    private Boolean pagopaPostgreStandInEnabled;

    @Value("${spring.datasource.nexi.postgre.standin.enabled}")
    private Boolean nexiPostgreStandInEnabled;

    @Value("${spring.datasource.nexi.oracle.standin.enabled}")
    private Boolean nexiOracleStandInEnabled;

    private final TransactionTemplate transactionTemplate;

    public StandInManagerService(@Value("${stand-in-manager.service.host}") String standInManagerUrl, ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
        transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void forceStandIn() {
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.standin);
            }
            log.debug("SyncService api-config-cache get stations");
            Response response = standInManagerClient.getCache(subscriptionKey);
            int httpResponseCode = response.status();
            if (httpResponseCode != HttpStatus.OK.value()) {
                log.error("SyncService stand-in-manager get stations error - result: httpStatusCode[{}]", httpResponseCode);
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }
            log.info("SyncService stand-in-manager get stations successful");

            List<StandInStations> stations = (List<StandInStations>) objectMapper.readValue(response.body().asInputStream().readAllBytes(), List.class);

            this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        if( pagopaPostgreStandInEnabled ) pagoPAStandInPostgreRepository.saveAll(stations);
//                        if( nexiPostgreStandInEnabled ) nexiStandInPostgreRepository.saveAll(stations);
//                        if( nexiOracleStandInEnabled ) nexiStandInOracleRepository.saveAll(stations);
                    } catch(NoSuchElementException ex) {
                        status.setRollbackOnly();
                    }
                }
            });
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService stand-in-manager get stations error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (FeignException | IOException e) {
            log.error("SyncService stand-in-manager get stations error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }
}
