package it.gov.pagopa.node.cfgsync;

import static it.gov.pagopa.node.cfgsync.util.Constants.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import feign.Request;
import feign.Response;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiCacheOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPACachePostgresRepository;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCachePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

class ApiConfigCachePersistenceServiceTest {

    @Mock
    private PagoPACachePostgresRepository pagoPACachePostgresRepository;
    @Mock
    private NexiCachePostgresRepository nexiCachePostgresRepository;
    @Mock
    private NexiCacheOracleRepository nexiCacheOracleRepository;

    @InjectMocks
    private ApiConfigCachePersistenceService service;

    private static final String PAGOPA_ID = "PAGOPAID";
    private static final String NEXI_POSTGRES_ID = "NEXIOID";
    private static final String NEXI_ORACLE_ID = "NEXIPID";

    private static final Map<String, Collection<String>> headers;
    static {
        headers = Map.of(
                HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())),
                HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()),
                HEADER_CACHE_VERSION, List.of("v1.0.0")
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service.setApiConfigCacheWritePagoPa(true);
        service.setApiConfigCacheWriteNexiOracle(true);
        service.setApiConfigCacheWriteNexiPostgres(true);

        ReflectionTestUtils.setField(service, "pagopaPostgresServiceIdentifier", PAGOPA_ID);
        ReflectionTestUtils.setField(service, "nexiPostgresServiceIdentifier", NEXI_POSTGRES_ID);
        ReflectionTestUtils.setField(service, "nexiOracleServiceIdentifier", NEXI_ORACLE_ID);
    }

    @Test
    void saveCache_success() {
        Map<String, SyncStatusEnum> expected = Map.of(
                PAGOPA_ID, SyncStatusEnum.DONE,
                NEXI_POSTGRES_ID, SyncStatusEnum.DONE,
                NEXI_ORACLE_ID, SyncStatusEnum.DONE
        );

        Response response = Response.builder()
                .status(200)
                .reason("Mocked")
                .headers(headers)
                .request(mock(Request.class))
                .body(new byte[0])
                .build();

        ConfigCache mockConfigCache = new ConfigCache();

        when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        when(nexiCacheOracleRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);

        Map<String, SyncStatusEnum> result = service.saveCache(response);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void saveCache_emptyHeadersThrowsException() {
        Response response = mock(Response.class);
        when(response.headers()).thenReturn(Collections.emptyMap());

        AppException ex = assertThrows(AppException.class, () -> service.saveCache(response));
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getHttpStatus(), ex.getHttpStatus());
    }

    @ParameterizedTest
    @MethodSource("provideSaveCacheScenarios")
    void saveCache_variousScenarios(
            boolean writePagoPa,
            boolean writeNexiOracle,
            boolean writeNexiPostgres,
            boolean pagoPaThrows,
            boolean nexiOracleThrows,
            boolean nexiPostgresThrows,
            Map<String, SyncStatusEnum> expected
    ) {
        Response response = Response.builder()
                .status(200)
                .reason("Mocked")
                .headers(headers)
                .request(mock(Request.class))
                .body(new byte[0])
                .build();

        ConfigCache mockConfigCache = new ConfigCache();

        if (pagoPaThrows) {
            when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenThrow(new RuntimeException("DB error"));
        } else {
            when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        }

        if (nexiOracleThrows) {
            when(nexiCacheOracleRepository.save(any(ConfigCache.class))).thenThrow(new RuntimeException("DB error"));
        } else {
            when(nexiCacheOracleRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        }

        if (nexiPostgresThrows) {
            when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenThrow(new RuntimeException("DB error"));
        } else {
            when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        }

        service.setApiConfigCacheWritePagoPa(writePagoPa);
        service.setApiConfigCacheWriteNexiOracle(writeNexiOracle);
        service.setApiConfigCacheWriteNexiPostgres(writeNexiPostgres);

        Map<String, SyncStatusEnum> result = service.saveCache(response);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideSaveCacheScenarios() {
        return Stream.of(
                // Caso: PagoPA lancia eccezione
                Arguments.of(true, true, true, true, false, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.ERROR,
                        NEXI_POSTGRES_ID, SyncStatusEnum.ROLLBACK,
                        NEXI_ORACLE_ID, SyncStatusEnum.ROLLBACK
                )),
                // Caso: PagoPA disabilitato
                Arguments.of(false, true, true, false, false, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.DISABLED,
                        NEXI_POSTGRES_ID, SyncStatusEnum.DONE,
                        NEXI_ORACLE_ID, SyncStatusEnum.DONE
                )),
                // Caso: Nexi Oracle lancia eccezione
                Arguments.of(true, true, true, false, true, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.ROLLBACK,
                        NEXI_ORACLE_ID, SyncStatusEnum.ERROR,
                        NEXI_POSTGRES_ID, SyncStatusEnum.ROLLBACK
                )),
                // Caso: Nexi Oracle disabilitato
                Arguments.of(true, false, true, false, false, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.DONE,
                        NEXI_ORACLE_ID, SyncStatusEnum.DISABLED,
                        NEXI_POSTGRES_ID, SyncStatusEnum.DONE
                )),
                // Caso: Nexi Postgres lancia eccezione
                Arguments.of(true, true, true, false, false, true, Map.of(
                        PAGOPA_ID, SyncStatusEnum.ROLLBACK,
                        NEXI_ORACLE_ID, SyncStatusEnum.ROLLBACK,
                        NEXI_POSTGRES_ID, SyncStatusEnum.ERROR
                )),
                // Caso: Nexi Postgres disabilitato
                Arguments.of(true, true, false, false, false, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.DONE,
                        NEXI_ORACLE_ID, SyncStatusEnum.DONE,
                        NEXI_POSTGRES_ID, SyncStatusEnum.DISABLED
                ))
        );
    }
}