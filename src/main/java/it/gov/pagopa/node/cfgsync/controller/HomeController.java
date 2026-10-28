package it.gov.pagopa.node.cfgsync.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.node.cfgsync.model.AppInfo;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
public class HomeController {

  private final String basePath;
  private final String name;
  private final String version;
  private final String environment;

  public HomeController(
      @Value("${server.servlet.context-path}") String basePath,
      @Value("${info.application.name}") String name,
      @Value("${info.application.version}") String version,
      @Value("${info.properties.environment}") String environment) {
    this.basePath = basePath;
    this.name = name;
    this.version = version;
    this.environment = environment;
  }

  /**
   * @return redirect to Swagger page documentation
   */
  @Hidden
  @GetMapping("")
  public RedirectView home() {
    String path = basePath;
    if (!basePath.endsWith("/")) {
      path += "/";
    }
    return new RedirectView(path + "swagger-ui.html");
  }

  /**
   * Health Check
   *
   * @return ok
   */
  @Operation(
      summary = "Return OK if application is started",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Home"})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppInfo.class))),
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
  @GetMapping("/info")
  public ResponseEntity<AppInfo> healthCheck() {

    AppInfo info = AppInfo.builder().name(name).version(version).environment(environment).build();
    return ResponseEntity.status(HttpStatus.OK).body(info);
  }
}
