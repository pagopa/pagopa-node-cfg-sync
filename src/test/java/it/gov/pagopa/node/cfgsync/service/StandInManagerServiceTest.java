package it.gov.pagopa.node.cfgsync.service;

import feign.FeignException;
import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.StationsResponse;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandInManagerServiceTest {

    private static final String PAGOPAPOSTGRES_SI = "PAGOPAPOSTGRES";
    private static final String NEXIPOSTGRES_SI = "NEXIPOSTGRES";
    private static final List<String> stations = List.of("1234567890", "9876543210");

    private StandInManagerService standInManagerService;

    @Mock private StandInManagerClient standInManagerClient;
    @Mock private PagoPAStandInPostgresRepository pagoPAStandInPostgresRepository;
    @Mock private NexiStandInPostgresRepository nexiStandInPostgresRepository;
    @Mock private AppInsightTelemetryClient telemetryClient;

    @BeforeEach
    void setUp() {
        standInManagerService = new StandInManagerService(
                true,  // standInManagerEnabled
                true,  // standInManagerWritePagoPa
                true,  // standInManagerWriteNexiPostgres
                3,     // saveMaxAttempts
                3,     // rollbackMaxAttempts
                standInManagerClient,
                pagoPAStandInPostgresRepository,
                nexiStandInPostgresRepository,
                telemetryClient,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                new TransactionTemplate(mock(PlatformTransactionManager.class))
        );
        ReflectionTestUtils.setField(standInManagerService, "pagopaPostgresServiceIdentifier", PAGOPAPOSTGRES_SI);
        ReflectionTestUtils.setField(standInManagerService, "nexiPostgresServiceIdentifier", NEXIPOSTGRES_SI);
    }

    @Test
    void error400() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerEnabled", false);

        AppException e = assertThrows(AppException.class, () -> standInManagerService.syncStandIn());

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        assertEquals("Target service disabled", e.getTitle());
        assertEquals("Target service stand-in-manager disabled", e.getMessage());

        ReflectionTestUtils.setField(standInManagerService, "standInManagerEnabled", true);
    }

    @Test
    void error500() {
        when(standInManagerClient.getStations()).thenReturn(null);

        AppException e = assertThrows(AppException.class, () -> standInManagerService.syncStandIn());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
        assertEquals("Internal Server Error", e.getTitle());
    }

    @Test
    void error500StandInManagerException() {
        when(standInManagerClient.getStations()).thenThrow(FeignException.class);

        AppException e = assertThrows(AppException.class, () -> standInManagerService.syncStandIn());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getHttpStatus());
        assertEquals("Internal Server Error", e.getTitle());
    }

    @Test
    void writePagoPAPostgresDisabled() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerWritePagoPa", false);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        Map<String, SyncStatusEnum> stringSyncStatusEnumMap = assertDoesNotThrow(() -> standInManagerService.syncStandIn());

        assertNotNull(stringSyncStatusEnumMap);
        assertFalse(stringSyncStatusEnumMap.isEmpty());
        assertEquals(2, stringSyncStatusEnumMap.size());
        assertNotNull(stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertEquals(SyncStatusEnum.DISABLED, stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertNotNull(stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));
        assertEquals(SyncStatusEnum.DONE, stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));

        ReflectionTestUtils.setField(standInManagerService, "standInManagerWritePagoPa", true);
    }

    @Test
    void writeNexiPostgresDisabled() {
        ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiPostgres", false);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        Map<String, SyncStatusEnum> stringSyncStatusEnumMap = assertDoesNotThrow(() -> standInManagerService.syncStandIn());

        assertNotNull(stringSyncStatusEnumMap);
        assertFalse(stringSyncStatusEnumMap.isEmpty());
        assertEquals(2, stringSyncStatusEnumMap.size());
        assertNotNull(stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertEquals(SyncStatusEnum.DONE, stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertNotNull(stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));
        assertEquals(SyncStatusEnum.DISABLED, stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));

        ReflectionTestUtils.setField(standInManagerService, "standInManagerWriteNexiPostgres", true);
    }

    @Test
    void errorWritePagoPAPostgres() {
        PagoPAStandInPostgresRepository repository = (PagoPAStandInPostgresRepository) ReflectionTestUtils.getField(standInManagerService, "pagoPAStandInPostgresRepository");
        ReflectionTestUtils.setField(standInManagerService, "pagoPAStandInPostgresRepository", null);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);


        Map<String, SyncStatusEnum> stringSyncStatusEnumMap = assertDoesNotThrow(() -> standInManagerService.syncStandIn());

        assertNotNull(stringSyncStatusEnumMap);
        assertFalse(stringSyncStatusEnumMap.isEmpty());
        assertEquals(2, stringSyncStatusEnumMap.size());
        assertNotNull(stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertEquals(SyncStatusEnum.ERROR, stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertNotNull(stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));
        assertEquals(SyncStatusEnum.ROLLBACK, stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));

        ReflectionTestUtils.setField(standInManagerService, "pagoPAStandInPostgresRepository", repository);
    }

    @Test
    void errorWriteNexiPostgres() {
        NexiStandInPostgresRepository repository = (NexiStandInPostgresRepository) ReflectionTestUtils.getField(standInManagerService, "nexiStandInPostgresRepository");
        ReflectionTestUtils.setField(standInManagerService, "nexiStandInPostgresRepository", null);

        StationsResponse stationsResponse = StationsResponse.builder().stations(stations).build();
        when(standInManagerClient.getStations()).thenReturn(stationsResponse);

        Map<String, SyncStatusEnum> stringSyncStatusEnumMap = assertDoesNotThrow(() -> standInManagerService.syncStandIn());

        assertNotNull(stringSyncStatusEnumMap);
        assertFalse(stringSyncStatusEnumMap.isEmpty());
        assertEquals(2, stringSyncStatusEnumMap.size());
        assertNotNull(stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertEquals(SyncStatusEnum.ROLLBACK, stringSyncStatusEnumMap.get(PAGOPAPOSTGRES_SI));
        assertNotNull(stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));
        assertEquals(SyncStatusEnum.ERROR, stringSyncStatusEnumMap.get(NEXIPOSTGRES_SI));

        ReflectionTestUtils.setField(standInManagerService, "nexiStandInPostgresRepository", repository);
    }


}
