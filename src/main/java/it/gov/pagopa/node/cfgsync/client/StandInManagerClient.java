package it.gov.pagopa.node.cfgsync.client;

import feign.RequestLine;
import it.gov.pagopa.node.cfgsync.config.StandInManagerFeignConfig;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "stand-in-manager", url = "${stand-in-manager.service.base-url}", configuration = StandInManagerFeignConfig.class)
public interface StandInManagerClient {

    @GetMapping(value = "/stations", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestLine("getStations")
    StationsResponse getStations();
}
