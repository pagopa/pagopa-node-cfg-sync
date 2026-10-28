package it.gov.pagopa.node.cfgsync.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;

import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_REQUEST_ID;

public abstract class BaseFeignConfig {

    @Bean
    public RequestInterceptor commonHeaderInterceptor() {
        return requestTemplate -> requestTemplate
                .header(HEADER_REQUEST_ID, MDC.get("requestId"));
    }
}
