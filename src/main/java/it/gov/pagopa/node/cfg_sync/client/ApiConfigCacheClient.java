package it.gov.pagopa.node.cfg_sync.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import org.springframework.stereotype.Service;

@Service
public interface ApiConfigCacheClient {

  @RequestLine("GET /cache")
  @Headers({
          "Ocp-Apim-Subscription-Key: {subscriptionKey}"
  })
  Response getCache(@Param String subscriptionKey);

}
