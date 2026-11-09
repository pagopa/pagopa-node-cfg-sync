package it.gov.pagopa.node.cfgsync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.controller.HomeController;
import it.gov.pagopa.node.cfgsync.controller.SyncCacheController;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheFetchService;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCachePersistenceService;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiGenerationTest {

  @Autowired ObjectMapper objectMapper;

  @Autowired private MockMvc mvc;

  @MockBean private HomeController homeController;
  @MockBean private SyncCacheController syncCacheController;
  @MockBean private ApiConfigCachePersistenceService apiConfigCachePersistenceService;
  @MockBean private ApiConfigCacheFetchService apiConfigCacheFetchService;
  @MockBean private ApiConfigCacheService apiConfigCacheService;
  @MockBean private StandInManagerService standInManagerService;
  @MockBean private TelemetryClient telemetryClient;
  @MockBean private AppInsightTelemetryClient appInsightTelemetryClient;

  @Test
  void swaggerSpringPlugin() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andDo(
            result -> {
              assertNotNull(result);
              assertNotNull(result.getResponse());
              final String content = result.getResponse().getContentAsString();
              assertFalse(content.isBlank());
              assertFalse(content.contains("${"), "Generated swagger contains placeholders");
              Object swagger =
                  objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
              String formatted =
                  objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
              Path basePath = Paths.get("openapi/");
              Files.createDirectories(basePath);
              Files.write(basePath.resolve("openapi.json"), formatted.getBytes());
            });
  }
}
