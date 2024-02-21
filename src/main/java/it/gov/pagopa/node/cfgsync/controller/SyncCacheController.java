package it.gov.pagopa.node.cfgsync.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.SyncStatusResponse;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ndp")
@Validated
public class SyncCacheController {

    @Autowired
    private ApiConfigCacheService apiConfigCacheService;
    @Autowired
    private StandInManagerService standInManagerService;


  @Operation(
      summary = "Force stand-in configuration update",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {
        "StandIn",
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
          value = "/stand-in",
          produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<SyncStatusResponse>> standin() {

      log.debug("Force stand-in configuration update");
      Map<String, SyncStatusEnum> syncStatusEnumMap = standInManagerService.forceStandIn();

      List<SyncStatusResponse> syncStatusResponseList = syncStatusEnumMap.entrySet()
              .stream()
              .map(e -> SyncStatusResponse.builder().serviceIdentifier(e.getKey()).status(e.getValue()).build())
              .collect(Collectors.toList());

      return ResponseEntity.ok()
              .body(syncStatusResponseList);
  }

    @Operation(
            summary = "Force cache configuration update",
            security = {@SecurityRequirement(name = "ApiKey")},
            tags = {
                    "Cache",
            })
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "OK",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = SyncStatusResponse.class)))
                            }),
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
            value = "/cache",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SyncStatusResponse>> cache() {
        log.debug("Force cache configuration update");
        Map<String, SyncStatusEnum> syncStatusEnumMap = apiConfigCacheService.forceCacheUpdate();

        List<SyncStatusResponse> syncStatusResponseList = syncStatusEnumMap.entrySet()
                .stream()
                .map(e -> SyncStatusResponse.builder().serviceIdentifier(e.getKey()).status(e.getValue()).build())
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .body(syncStatusResponseList);
    }

}
