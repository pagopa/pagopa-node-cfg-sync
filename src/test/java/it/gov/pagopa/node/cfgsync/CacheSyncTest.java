package it.gov.pagopa.node.cfgsync;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import feign.Feign;
import feign.FeignException;
import feign.Request;
import feign.mock.MockClient;
import feign.mock.MockTarget;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheEhConsumer;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
  @Autowired private TestRestTemplate restTemplate;
  private MockClient mockClient;

  @LocalServerPort private int port;

  @Mock
  ApiConfigCacheClient apiConfigCacheClient;

  @Test
  void syncCache_400() {
    ReflectionTestUtils.setField(cacheManagerService, "enabled", false);
    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getTitle()).isEqualTo("Target service disabled");
    assertThat(response.getBody().getStatus()).isEqualTo(400);
    assertThat(response.getBody().getDetail()).isEqualTo("Target service api-config-cache disabled");
    ReflectionTestUtils.setField(cacheManagerService, "enabled", true);
  }

  @Test
  void syncCache_500() {
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
  void syncCache_500_ConnectionRefused() {
//    mockClient = new MockClient().noContent(feign.mock.HttpMethod.GET, "/stations");
//    standInManagerClient =
//            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
//    standInManagerService.setStandInManagerClient(standInManagerClient);

    Request request = mock(Request.class);
    when(apiConfigCacheClient.getCache(anyString()))
            .thenThrow(new FeignException.NotFound("message", request, null, null));

    ResponseEntity<ProblemJson> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

//  @Test
//  void syncCache_WritePagoPAPostgresDisabled() throws Exception {
//    ObjectMapper objectMapper = new ObjectMapper();
//
////    HttpHeaders headers = new HttpHeaders();
////    headers.put(HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())));
////    headers.put(HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()));
////    headers.put(HEADER_CACHE_VERSION, List.of("v1.0.0"));
//
//    Map<String, Collection<String>> headers = new HashMap<>();
//    headers.put(HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())));
//    headers.put(HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()));
//    headers.put(HEADER_CACHE_VERSION, List.of("v1.0.0"));
//
////    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, CLIENT_CACHE_PATH, ResponseEntity.ok().headers(headers).build().toString());
//
//    Request request = mock(Request.class);
//    mockClient = new MockClient()
//            .ok(
//                    feign.mock.HttpMethod.GET,
//                    CLIENT_CACHE_PATH,
//                    Response
//                            .builder()
//                            .headers(headers)
//                            .request(request)
//                            .body(new byte[0])
//                            .build().toString());
////    mockClient = new MockClient()
////            .ok(
////                    feign.mock.HttpMethod.GET,
////                    CLIENT_CACHE_PATH,
////                    Response
////                            .builder()
////                            .status(200)
////                            .reason("Mocked")
////                            .headers(headers)
////                            .body(new byte[0])
////                            .build()
////                            .toString());
//    ApiConfigCacheClient apiConfigCacheClient =
//            Feign.builder().client(mockClient).target(new MockTarget<>(ApiConfigCacheClient.class));
//    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);
//    ReflectionTestUtils.setField(cacheManagerService, "writePagoPa", false);
//
//    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});
//
//    assertThat(response.getBody()).isNotNull();
//    assertFalse(response.getHeaders().isEmpty());
//    assertEquals(3, response.getBody());
//    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo("NDP001");
//    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.disabled);
//    ReflectionTestUtils.setField(cacheManagerService, "writePagoPa", true);
//  }

//  @Test
//  void syncCache_WriteNexiOracleDisabled() throws Exception {
//    ObjectMapper objectMapper = new ObjectMapper();
//    StationsResponse stationsResponse = StationsResponse.builder().stations(List.of("1234567890", "9876543210")).build();
//
//    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, CLIENT_CACHE_PATH, objectMapper.writeValueAsBytes(stationsResponse));
//    ApiConfigCacheClient apiConfigCacheClient =
//            Feign.builder().client(mockClient).target(new MockTarget<>(ApiConfigCacheClient.class));
//    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);
//    ReflectionTestUtils.setField(cacheManagerService, "writeNexiOracle", false);
//
//    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});
//
//    List<SyncStatusResponse> syncStatusResponseList = response.getBody();
//
//    assertThat(syncStatusResponseList).isNotNull();
//    assertFalse(syncStatusResponseList.isEmpty());
//    assertEquals(3, syncStatusResponseList.size());
//    assertThat(syncStatusResponseList.get(2).getServiceIdentifier()).isEqualTo("NDP003");
//    assertThat(syncStatusResponseList.get(2).getStatus()).isEqualTo(SyncStatusEnum.disabled);
//    ReflectionTestUtils.setField(cacheManagerService, "writeNexiOracle", true);
//  }

//  @Test
//  void syncCache_WriteNexiPostgresDisabled() throws Exception {
//    ObjectMapper objectMapper = new ObjectMapper();
//    StationsResponse stationsResponse = StationsResponse.builder().stations(List.of("1234567890", "9876543210")).build();
//
//    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, CLIENT_CACHE_PATH, objectMapper.writeValueAsBytes(stationsResponse));
//    ApiConfigCacheClient apiConfigCacheClient =
//            Feign.builder().client(mockClient).target(new MockTarget<>(ApiConfigCacheClient.class));
//    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);
//    ReflectionTestUtils.setField(cacheManagerService, "writeNexiPostgres", false);
//
//    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});
//
//    List<SyncStatusResponse> syncStatusResponseList = response.getBody();
//
//    assertThat(syncStatusResponseList).isNotNull();
//    assertFalse(syncStatusResponseList.isEmpty());
//    assertEquals(3, syncStatusResponseList.size());
//    assertThat(syncStatusResponseList.get(1).getServiceIdentifier()).isEqualTo("NDP004DEV");
//    assertThat(syncStatusResponseList.get(1).getStatus()).isEqualTo(SyncStatusEnum.disabled);
//
//    ReflectionTestUtils.setField(cacheManagerService, "writeNexiPostgres", true);
//  }

}
