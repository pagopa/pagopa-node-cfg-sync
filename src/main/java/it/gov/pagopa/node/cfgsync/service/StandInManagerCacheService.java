package it.gov.pagopa.node.cfgsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.client.model.StandInManagerResponse;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Service
@Slf4j
public class StandInManagerCacheService extends CommonCacheService implements CacheService {

    @Value("${service.stand-in-manager.enabled}")
    private boolean enabled;
    @Value("${service.stand-in-manager.subscriptionKey}")
    private String subscriptionKey;
    @Value("${stand-in-rx-connection-string}")
    private String standInRxConnectionString;
    @Value("${stand-in-rx-name}")
    private String standInRxName;
    @Value("${stand-in-sa-connection-string}")
    private String standInSaConnectionString;
    @Value("${stand-in-sa-name}")
    private String standInSaContainerName;
    @Value("${stand-in-consumer-group}")
    private String standInConsumerGroup;

    private final StandInManagerClient standInManagerClient;

    public StandInManagerCacheService(@Value("${service.stand-in-manager.host}") String standInManagerUrl) {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
    }

    @Override
    public TargetRefreshEnum getType() {
        return TargetRefreshEnum.standin;
    }

    @Override
    public void sync() {
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, getType());
            }
            log.debug("SyncService stand-in-manager get cache");
            Response response = standInManagerClient.getCache(subscriptionKey);
            int httpResponseCode = response.status();
            if (httpResponseCode != HttpStatus.OK.value()) {
                log.error("SyncService stand-in-manager get stations error - result: httpStatusCode[{}]", httpResponseCode);
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }
            log.info("SyncService stand-in-manager get stations successful");

            Map<String, Collection<String>> headers = response.headers();
            if( headers.isEmpty() ) {
                log.error("SyncService api-config-cache get cache error - empty header");
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }

            StandInManagerResponse standInManagerResponse = new ObjectMapper().readValue(response.body().asInputStream(), StandInManagerResponse.class);
            //TODO: chiamare repository per salvare le stazioni
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService stand-in-manager get cache error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (FeignException | IOException e) {
            log.error("SyncService api-config-cache get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }
}
