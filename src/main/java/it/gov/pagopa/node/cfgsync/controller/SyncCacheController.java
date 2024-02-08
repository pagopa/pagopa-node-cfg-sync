package it.gov.pagopa.node.cfgsync.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import it.gov.pagopa.node.cfgsync.model.RefreshResponse;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.service.CacheServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/sync")
@Validated
public class SyncCacheController {

    @Autowired
    private CacheServiceFactory cacheServiceFactory;


  @Operation(
      summary = "Sync target v1 config",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {
        "Cache",
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE
                )),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests",
            content = @Content(schema = @Schema())),
        @ApiResponse(
            responseCode = "500",
            description = "Service unavailable",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemJson.class)))
      })
  @GetMapping(
      value = "/v1/{target}",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<RefreshResponse> cache(@PathVariable TargetRefreshEnum target) {

      log.debug("Sync {} configuration", target.label);
      CacheServiceFactory.getService(target).sync();

      String requestId = UUID.randomUUID().toString();
      ZonedDateTime timestamp = ZonedDateTime.now();

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.set("X-REQUEST-ID", requestId);
      responseHeaders.set("X-CACHE-TIMESTAMP", DateTimeFormatter.ISO_DATE_TIME.format(timestamp));

      return ResponseEntity.ok()
              .headers(responseHeaders)
              .body(RefreshResponse.builder().timestamp(timestamp).id(requestId).build());
  }

}
