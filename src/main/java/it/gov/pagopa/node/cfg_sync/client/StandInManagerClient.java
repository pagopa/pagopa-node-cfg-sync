package it.gov.pagopa.node.cfg_sync.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import org.springframework.stereotype.Service;

@Service
public interface StandInManagerClient {

  @RequestLine("GET /cache/refresh")
  @Headers({
          "Content-Type: application/json",
          "Ocp-Apim-Subscription-Key: {subscriptionKey}"
  })
  Response refresh(@Param String subscriptionKey);

}
