package it.gov.pagopa.node.cfgsync.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static it.gov.pagopa.node.cfgsync.util.Constants.APIM_SUBSCRIPTION_KEY;

public class StandInManagerFeignConfig extends BaseFeignConfig {

    @Value("${stand-in-manager.service.subscriptionKey}")
    private String apiConfigCacheSubscriptionKey;

    @Bean
    public RequestInterceptor subscriptionKey() {
        return requestTemplate -> requestTemplate.header(APIM_SUBSCRIPTION_KEY, apiConfigCacheSubscriptionKey);
    }
}
