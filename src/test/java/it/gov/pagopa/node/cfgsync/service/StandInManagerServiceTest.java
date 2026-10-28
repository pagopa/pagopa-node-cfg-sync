package it.gov.pagopa.node.cfgsync.service;

import feign.FeignException;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.model.ProblemJson;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.SyncStatusResponse;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiStandInOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@WebMvcTest
@Import({StandInManagerService.class})
class StandInManagerServiceTest {

    private static final String STANDIN_URL = "/ndp/stand-in";
    private static final String PAGOPAPOSTGRES_SI = "PAGOPAPOSTGRES";
    private static final String NEXIPOSTGRES_SI = "NEXIPOSTGRES";
    private static final String NEXIORACLE_SI = "NEXIORACLE";
    private static final List<String> stations = List.of("1234567890", "9876543210");

    @Autowired
    private StandInManagerService standInManagerService;
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @MockBean
    StandInManagerClient standInManagerClient;

    @Test
    void error400() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerEnabled", false);

        ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Target service disabled", response.getBody().getTitle());
        assertEquals("Target service stand-in-manager disabled", response.getBody().getDetail());

        ReflectionTestUtils.setField(standInManagerService, "standInManagerEnabled", true);
    }

    @Test
    void error500() {
        when(standInManagerClient.getStations()).thenReturn(null);

        ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getTitle());
    }

    @Test
    void error500StandInManagerException() {
        when(standInManagerClient.getStations()).thenThrow(FeignException.class);

        ResponseEntity<ProblemJson> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, ProblemJson.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getTitle());
    }

    @Test
    void writePagoPAPostgresDisabled() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerWritePagoPa", false);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {
        });

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        assertEquals(3, response.getBody().size());
        assertEquals(PAGOPAPOSTGRES_SI, response.getBody().get(0).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DISABLED, response.getBody().get(0).getStatus());
        assertEquals(NEXIPOSTGRES_SI, response.getBody().get(1).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DONE, response.getBody().get(1).getStatus());
        assertEquals(NEXIORACLE_SI, response.getBody().get(2).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DONE, response.getBody().get(2).getStatus());

        ReflectionTestUtils.setField(standInManagerService, "standInManagerWritePagoPa", true);
    }

    @Test
    void writeNexiOracleDisabled() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiOracle", false);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {
        });

        assertNotNull(response.getBody());
        List<SyncStatusResponse> syncStatusResponseList = response.getBody();

        assertNotNull(syncStatusResponseList);
        assertFalse(syncStatusResponseList.isEmpty());
        assertEquals(3, syncStatusResponseList.size());
        assertEquals(PAGOPAPOSTGRES_SI, response.getBody().get(0).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DONE, response.getBody().get(0).getStatus());
        assertEquals(NEXIPOSTGRES_SI, response.getBody().get(1).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DONE, response.getBody().get(1).getStatus());
        assertEquals(NEXIORACLE_SI, response.getBody().get(2).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DISABLED, response.getBody().get(2).getStatus());

        ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiOracle", true);
    }

    @Test
    void writeNexiPostgresDisabled() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiPostgres", false);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {
        });

        assertNotNull(response.getBody());
        List<SyncStatusResponse> syncStatusResponseList = response.getBody();

        assertNotNull(syncStatusResponseList);
        assertFalse(syncStatusResponseList.isEmpty());
        assertEquals(3, syncStatusResponseList.size());
        assertEquals(PAGOPAPOSTGRES_SI, response.getBody().get(0).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DONE, response.getBody().get(0).getStatus());
        assertEquals(NEXIPOSTGRES_SI, response.getBody().get(1).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DISABLED, response.getBody().get(1).getStatus());
        assertEquals(NEXIORACLE_SI, response.getBody().get(2).getServiceIdentifier());
        assertEquals(SyncStatusEnum.DONE, response.getBody().get(2).getStatus());

        ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiPostgres", true);
    }

    @Test
    void errorWritePagoPAPostgres() {
        PagoPAStandInPostgresRepository repository = (PagoPAStandInPostgresRepository) ReflectionTestUtils.getField(standInManagerService, "pagoPAStandInPostgresRepository");
        ReflectionTestUtils.setField(standInManagerService, "pagoPAStandInPostgresRepository", null);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {
        });

        assertNotNull(response.getBody());
        List<SyncStatusResponse> syncStatusResponseList = response.getBody();

        assertNotNull(syncStatusResponseList);
        assertFalse(syncStatusResponseList.isEmpty());
        assertEquals(3, syncStatusResponseList.size());
        assertEquals(PAGOPAPOSTGRES_SI, response.getBody().get(0).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ERROR, response.getBody().get(0).getStatus());
        assertEquals(NEXIPOSTGRES_SI, response.getBody().get(1).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ROLLBACK, response.getBody().get(1).getStatus());
        assertEquals(NEXIORACLE_SI, response.getBody().get(2).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ROLLBACK, response.getBody().get(2).getStatus());

        ReflectionTestUtils.setField(standInManagerService, "pagoPAStandInPostgresRepository", repository);
    }

    @Test
    void errorWriteNexiPostgres() {
        NexiStandInPostgresRepository repository = (NexiStandInPostgresRepository) ReflectionTestUtils.getField(standInManagerService, "nexiStandInPostgresRepository");
        ReflectionTestUtils.setField(standInManagerService, "nexiStandInPostgresRepository", null);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {
        });

        assertNotNull(response.getBody());
        List<SyncStatusResponse> syncStatusResponseList = response.getBody();

        assertNotNull(syncStatusResponseList);
        assertFalse(syncStatusResponseList.isEmpty());
        assertEquals(3, syncStatusResponseList.size());
        assertEquals(PAGOPAPOSTGRES_SI, response.getBody().get(0).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ROLLBACK, response.getBody().get(0).getStatus());
        assertEquals(NEXIPOSTGRES_SI, response.getBody().get(1).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ERROR, response.getBody().get(1).getStatus());
        assertEquals(NEXIORACLE_SI, response.getBody().get(2).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ROLLBACK, response.getBody().get(2).getStatus());

        ReflectionTestUtils.setField(standInManagerService, "nexiStandInPostgresRepository", repository);
    }

    @Test
    void errorWriteNexiOracle() {
        NexiStandInOracleRepository repository = (NexiStandInOracleRepository) ReflectionTestUtils.getField(standInManagerService, "nexiStandInOracleRepository");
        ReflectionTestUtils.setField(standInManagerService, "nexiStandInOracleRepository", null);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(STANDIN_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {
        });

        assertNotNull(response.getBody());
        List<SyncStatusResponse> syncStatusResponseList = response.getBody();

        assertNotNull(syncStatusResponseList);
        assertFalse(syncStatusResponseList.isEmpty());
        assertEquals(3, syncStatusResponseList.size());
        assertEquals(PAGOPAPOSTGRES_SI, response.getBody().get(0).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ROLLBACK, response.getBody().get(0).getStatus());
        assertEquals(NEXIPOSTGRES_SI, response.getBody().get(1).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ROLLBACK, response.getBody().get(1).getStatus());
        assertEquals(NEXIORACLE_SI, response.getBody().get(2).getServiceIdentifier());
        assertEquals(SyncStatusEnum.ERROR, response.getBody().get(2).getStatus());

        ReflectionTestUtils.setField(standInManagerService, "nexiStandInOracleRepository", repository);
    }

}
