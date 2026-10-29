package it.gov.pagopa.node.cfgsync.client;

import feign.FeignException;
import it.gov.pagopa.node.cfgsync.config.ApiConfigCacheFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "api-config-cache", url = "${api-config-cache.service.base-url}", configuration = ApiConfigCacheFeignConfig.class)
public interface ApiConfigCacheClient {

    @Retryable(
            exclude = FeignException.FeignClientException.class,
            maxAttemptsExpression = "${api-config-cache.service.retryLeft}",
            backoff = @Backoff(delayExpression = "${api-config-cache.service.attemptDelay}"))
    @GetMapping(value = "/stakeholders/node/cache/schemas/v1")
    ResponseEntity<byte[]> getCache();
}
