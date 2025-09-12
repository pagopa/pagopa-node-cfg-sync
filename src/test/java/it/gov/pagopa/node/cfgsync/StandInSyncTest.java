package it.gov.pagopa.node.cfgsync;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.mock.MockClient;
import feign.mock.MockTarget;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.SyncStatusResponse;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiStandInOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static it.gov.pagopa.node.cfgsync.ConstantsHelper.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class StandInSyncTest {

  public static final String STANDIN_URL = "/ndp/stand-in";
  public static final String STATIONS_PATH = "/stations";

  static final List<String> stations = List.of("1234567890", "9876543210");

  @Autowired private StandInManagerService standInManagerService;
  @Autowired private TestRestTemplate restTemplate;
  private MockClient mockClient;

  @LocalServerPort private int port;

  @Mock
  StandInManagerClient standInManagerClient;

  ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void error400() {
    ReflectionTestUtils.setField(standInManagerService, "standInManagerEnabled", false);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getStatus()).isEqualTo(400);
    assertThat(response.getBody().getTitle()).isEqualTo("Target service disabled");
    assertThat(response.getBody().getDetail()).isEqualTo("Target service stand-in-manager disabled");

    ReflectionTestUtils.setField(standInManagerService, "standInManagerEnabled", true);
  }

  @Test
  void error500() {
    mockClient = new MockClient().noContent(feign.mock.HttpMethod.GET, STATIONS_PATH);
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void error500ClientNull() {
    mockClient = new MockClient().noContent(feign.mock.HttpMethod.GET, STATIONS_PATH);

    standInManagerService.setStandInManagerClient(null);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void error500StandInManagerException() {
    when(standInManagerClient.getCache(anyString())).thenThrow(FeignException.class);

    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void writePagoPAPostgresDisabled() throws Exception {
    ReflectionTestUtils.setField(standInManagerService, "standInManagerWritePagoPa", false);

    StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();

    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, STATIONS_PATH, objectMapper.writeValueAsBytes(stationsResponse));
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DISABLED);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    ReflectionTestUtils.setField(standInManagerService, "standInManagerWritePagoPa", true);
  }

  @Test
  void writeNexiOracleDisabled() throws Exception {
    ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiOracle", false);

    StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();

    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, STATIONS_PATH, objectMapper.writeValueAsBytes(stationsResponse));
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    List<SyncStatusResponse> syncStatusResponseList = response.getBody();

    assertThat(syncStatusResponseList).isNotNull();
    assertFalse(syncStatusResponseList.isEmpty());
    assertEquals(3, syncStatusResponseList.size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DISABLED);

    ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiOracle", true);
  }

  @Test
  void writeNexiPostgresDisabled() throws Exception {
    ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiPostgres", false);

    StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();

    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, STATIONS_PATH, objectMapper.writeValueAsBytes(stationsResponse));
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    List<SyncStatusResponse> syncStatusResponseList = response.getBody();

    assertThat(syncStatusResponseList).isNotNull();
    assertFalse(syncStatusResponseList.isEmpty());
    assertEquals(3, syncStatusResponseList.size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DISABLED);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);

    ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiPostgres", true);
  }

  @Test
  void errorWritePagoPAPostgres() throws Exception {
    PagoPAStandInPostgresRepository repository = (PagoPAStandInPostgresRepository)ReflectionTestUtils.getField(standInManagerService, "pagoPAStandInPostgresRepository");
    ReflectionTestUtils.setField(standInManagerService, "pagoPAStandInPostgresRepository", null);

    StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();

    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, STATIONS_PATH, objectMapper.writeValueAsBytes(stationsResponse));
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.ERROR);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);

    ReflectionTestUtils.setField(standInManagerService, "pagoPAStandInPostgresRepository", repository);
  }

  @Test
  void errorWriteNexiPostgres() throws Exception {
    NexiStandInPostgresRepository repository = (NexiStandInPostgresRepository)ReflectionTestUtils.getField(standInManagerService, "nexiStandInPostgresRepository");
    ReflectionTestUtils.setField(standInManagerService, "nexiStandInPostgresRepository", null);

    StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();

    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, STATIONS_PATH, objectMapper.writeValueAsBytes(stationsResponse));
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.ERROR);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);

    ReflectionTestUtils.setField(standInManagerService, "nexiStandInPostgresRepository", repository);
  }

  @Test
  void errorWriteNexiOracle() throws Exception {
    NexiStandInOracleRepository repository = (NexiStandInOracleRepository)ReflectionTestUtils.getField(standInManagerService, "nexiStandInOracleRepository");
    ReflectionTestUtils.setField(standInManagerService, "nexiStandInOracleRepository", null);

    StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();

    mockClient = new MockClient().ok(feign.mock.HttpMethod.GET, STATIONS_PATH, objectMapper.writeValueAsBytes(stationsResponse));
    StandInManagerClient standInManagerClient =
            Feign.builder().client(mockClient).target(new MockTarget<>(StandInManagerClient.class));
    standInManagerService.setStandInManagerClient(standInManagerClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.ROLLBACK);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.ERROR);

    ReflectionTestUtils.setField(standInManagerService, "nexiStandInOracleRepository", repository);
  }

}
