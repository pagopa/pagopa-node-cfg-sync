package it.gov.pagopa.node.cfgsync.service;

import feign.Feign;
import feign.FeignException;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StandInManagerCacheService extends CommonCacheService implements CacheService {

    @Value("${service.stand-in-manager.enabled}") private boolean enabled;
    @Value("${service.stand-in-manager.subscriptionKey}") private String subscriptionKey;

    private final StandInManagerClient standInManagerClient;

    public StandInManagerCacheService(@Value("${service.stand-in-manager.host}") String standInManagerUrl) {
        standInManagerClient = Feign.builder().target(StandInManagerClient.class, standInManagerUrl);
    }

    @Override
    public TargetRefreshEnum getType() {
        return TargetRefreshEnum.standin;
    }

    @Override
    public void syncCache() {
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, getType());
            }
            log.debug("SyncService stand-in-manager get cache");
//            Response response = standInManagerClient.refresh(subscriptionKey);
//            int httpResponseCode = response.status();
//            if (httpResponseCode != HttpStatus.OK.value()) {
//                log.error("SyncService stand-in-manager get cache error - result: httpStatusCode[{}]", httpResponseCode);
//            } else {
//                log.info("SyncService stand-in-manager get cache successful");
//            }
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService stand-in-manager get cache error: Gateway timeout", e);
        } catch (FeignException e) {
            log.error("SyncService stand-in-manager get cache error", e);
        }
    }
}
