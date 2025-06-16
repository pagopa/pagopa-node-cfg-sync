package it.gov.pagopa.node.cfgsync;

import feign.Feign;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.mock.MockClient;
import feign.mock.MockTarget;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.SyncStatusResponse;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiCacheOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPACachePostgresRepository;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import it.gov.pagopa.node.cfgsync.service.CommonCacheService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.node.cfgsync.ConstantsHelper.*;
import static it.gov.pagopa.node.cfgsync.util.Constants.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
class CacheSyncTest {

  public static final String CACHE_URL = "/ndp/cache";
  public static final String CLIENT_CACHE_PATH = "/cache";

  @Autowired private ApiConfigCacheService cacheManagerService;
  @Autowired private CommonCacheService commonCacheService;
  @Autowired private TestRestTemplate restTemplate;
  private MockClient mockClient;

  @LocalServerPort private int port;

  @Mock
  ApiConfigCacheClient apiConfigCacheClient;

  private static final Map<String, Collection<String>> headers;
  static {
      headers = Map.of(HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())), HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()), HEADER_CACHE_VERSION, List.of("v1.0.0"));
  }

  @Test
  void error400() {
    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheServiceEnabled", false);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getTitle()).isEqualTo("Target service disabled");
    assertThat(response.getBody().getStatus()).isEqualTo(400);
    assertThat(response.getBody().getDetail()).isEqualTo("Target service api-config-cache disabled");

    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheServiceEnabled", true);
  }

  @Test
  void error500() {
    mockClient = new MockClient().noContent(feign.mock.HttpMethod.GET, CLIENT_CACHE_PATH);
    ApiConfigCacheClient apiConfigCacheClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(ApiConfigCacheClient.class));
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void error500ApiConfigCacheException() {
    when(apiConfigCacheClient.getCache(anyString())).thenThrow(FeignException.class);

    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void error500NullCacheHeader() {
    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(null)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());

    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void error500EmptyKeyCacheHeader() {
    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(Map.of(HEADER_CACHE_ID, Collections.emptyList(), HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()), HEADER_CACHE_VERSION, List.of("v1.0.0")))
            .request(mock(Request.class))
            .body(new byte[0])
            .build());

    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void error500ConnectionRefused() {
    Request request = mock(Request.class);
    when(apiConfigCacheClient.getCache(anyString()))
            .thenThrow(new FeignException.NotFound("message", request, null, null));

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void trimCacheVersionOnDb() {

    Map<String, Collection<String>> headersCustom =
            Map.of(
                    HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())),
                    HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()),
                    HEADER_CACHE_VERSION, List.of(StringUtils.repeat("*", 50))
            );
    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headersCustom)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getHeaders().isEmpty());
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);
  }

  @Test
  void writePagoPAPostgresDisabled() {
    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheWritePagoPa", false);

    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
                    .builder()
                    .status(200)
                    .reason("Mocked")
                    .headers(headers)
                    .request(mock(Request.class))
                    .body(new byte[0])
                    .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getHeaders().isEmpty());
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DISABLED);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);

    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheWritePagoPa", true);
  }

  @Test
  void writeNexiOracleDisabled() {
    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheWriteNexiOracle", false);

    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headers)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());

    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getHeaders().isEmpty());
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DISABLED);

    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheWriteNexiOracle", true);
  }

  @Test
  void writeNexiPostgresDisabled() {
    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheWriteNexiPostgres", false);

    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headers)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());

    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getHeaders().isEmpty());
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DISABLED);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);

    ReflectionTestUtils.setField(cacheManagerService, "apiConfigCacheWriteNexiPostgres", true);
  }

  @Test
  void errorWritePagoPAPostgres() {
    PagoPACachePostgresRepository repository = (PagoPACachePostgresRepository)ReflectionTestUtils.getField(cacheManagerService, "pagoPACachePostgresRepository");
    ReflectionTestUtils.setField(cacheManagerService, "pagoPACachePostgresRepository", null);

    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headers)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.ERROR);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);

    ReflectionTestUtils.setField(cacheManagerService, "pagoPACachePostgresRepository", repository);
  }

  @Test
  void errorWriteNexiPostgres() {
    NexiCachePostgresRepository repository = (NexiCachePostgresRepository)ReflectionTestUtils.getField(cacheManagerService, "nexiCachePostgresRepository");
    ReflectionTestUtils.setField(cacheManagerService, "nexiCachePostgresRepository", null);

    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headers)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.ERROR);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);

    ReflectionTestUtils.setField(cacheManagerService, "nexiCachePostgresRepository", repository);
  }

  @Test
  void errorWriteNexiOracle() {
    NexiCacheOracleRepository repository = (NexiCacheOracleRepository)ReflectionTestUtils.getField(cacheManagerService, "nexiCacheOracleRepository");
    ReflectionTestUtils.setField(cacheManagerService, "nexiCacheOracleRepository", null);

    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headers)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.ERROR);
    ReflectionTestUtils.setField(cacheManagerService, "nexiCacheOracleRepository", repository);
  }

}
