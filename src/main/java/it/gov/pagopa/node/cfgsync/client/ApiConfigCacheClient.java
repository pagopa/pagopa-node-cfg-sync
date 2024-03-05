package it.gov.pagopa.node.cfgsync.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import org.springframework.stereotype.Service;

@Service
public interface ApiConfigCacheClient {

  @RequestLine("GET /")
  @Headers({
          "Ocp-Apim-Subscription-Key: {subscriptionKey}"
  })
  Response getCache(@Param String subscriptionKey);

}
