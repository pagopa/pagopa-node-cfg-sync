package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPACachePostgresRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_ID;
import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_TIMESTAMP;
import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ApiConfigCachePersistenceServiceTest {

    @Mock
    private PagoPACachePostgresRepository pagoPACachePostgresRepository;
    @Mock
    private NexiCachePostgresRepository nexiCachePostgresRepository;
    @Mock
    private AppInsightTelemetryClient telemetryClient;

    private ApiConfigCachePersistenceService service;

    private static final String PAGOPA_ID = "PAGOPAID";
    private static final String NEXI_POSTGRES_ID = "NEXIPID";

    @Test
    void saveCache_success() {
        service = buildService(true, true);

        Map<String, SyncStatusEnum> expected = Map.of(
                PAGOPA_ID, SyncStatusEnum.DONE,
                NEXI_POSTGRES_ID, SyncStatusEnum.DONE
        );

        ConfigCache mockConfigCache = new ConfigCache();
        when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);

        Map<String, SyncStatusEnum> result = service.saveCache(okResponse());

        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void saveCache_emptyHeadersThrowsException() {
        service = buildService(true, true);

        @SuppressWarnings("unchecked")
        ResponseEntity<byte[]> response = mock(ResponseEntity.class);
        when(response.getHeaders()).thenReturn(new HttpHeaders());

        AppException ex = assertThrows(AppException.class, () -> service.saveCache(response));
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getHttpStatus(), ex.getHttpStatus());
    }

    @Test
    void saveCache_duplicateKey_isIdempotentDoneAndNoCompensation() {
        service = buildService(true, true);

        ConfigCache mockConfigCache = new ConfigCache();
        // PagoPA already contains the cache id -> idempotent skip, kept DONE, never compensated
        when(pagoPACachePostgresRepository.save(any(ConfigCache.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));
        when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);

        Map<String, SyncStatusEnum> result = service.saveCache(okResponse());

        assertEquals(Map.of(
                PAGOPA_ID, SyncStatusEnum.DONE,
                NEXI_POSTGRES_ID, SyncStatusEnum.DONE
        ), result);
        // No database operation must be performed for the already-present record
        verify(pagoPACachePostgresRepository, never()).deleteById(anyString());
    }

    @Test
    void saveCache_rollbackFailure_keepsErrorWithAlert() {
        service = buildService(true, true);

        ConfigCache mockConfigCache = new ConfigCache();
        // PagoPA writes ok, Nexi Postgres fails -> compensation of PagoPA
        when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
        when(nexiCachePostgresRepository.save(any(ConfigCache.class)))
                .thenThrow(new RuntimeException("DB error"));
        // The compensating delete on PagoPA keeps failing -> inconsistency -> ERROR + critical alert
        doThrow(new RuntimeException("delete failed")).when(pagoPACachePostgresRepository).deleteById(anyString());

        Map<String, SyncStatusEnum> result = service.saveCache(okResponse());

        assertEquals(Map.of(
                PAGOPA_ID, SyncStatusEnum.ERROR,        // rollback failed -> stays ERROR
                NEXI_POSTGRES_ID, SyncStatusEnum.ERROR  // original failure
        ), result);
        verify(telemetryClient, atLeastOnce())
                .createCustomEventForAlert(eq(AppError.CACHE_ROLLBACK_FAILED), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("provideSaveCacheScenarios")
    void saveCache_variousScenarios(
            boolean writePagoPa,
            boolean writeNexiPostgres,
            boolean pagoPaThrows,
            boolean nexiPostgresThrows,
            Map<String, SyncStatusEnum> expected
    ) {
        service = buildService(writePagoPa, writeNexiPostgres);

        ConfigCache mockConfigCache = new ConfigCache();

        if (writePagoPa) {
            if (pagoPaThrows) {
                lenient().when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenThrow(new RuntimeException("DB error"));
            } else {
                lenient().when(pagoPACachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
            }
        }

        if (writeNexiPostgres) {
            if (nexiPostgresThrows) {
                lenient().when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenThrow(new RuntimeException("DB error"));
            } else {
                lenient().when(nexiCachePostgresRepository.save(any(ConfigCache.class))).thenReturn(mockConfigCache);
            }
        }

        Map<String, SyncStatusEnum> result = service.saveCache(okResponse());

        assertNotNull(result);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideSaveCacheScenarios() {
        return Stream.of(
                // Caso: PagoPA lancia eccezione -> fail-fast, Nexi Postgres non viene scritto (ROLLBACK)
                Arguments.of(true, true, true, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.ERROR,
                        NEXI_POSTGRES_ID, SyncStatusEnum.ROLLBACK
                )),
                // Caso: PagoPA disabilitato
                Arguments.of(false, true, false, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.DISABLED,
                        NEXI_POSTGRES_ID, SyncStatusEnum.DONE
                )),
                // Caso: Nexi Postgres lancia eccezione -> PagoPA, gia inserito, viene compensato
                Arguments.of(true, true, false, true, Map.of(
                        PAGOPA_ID, SyncStatusEnum.ROLLBACK,
                        NEXI_POSTGRES_ID, SyncStatusEnum.ERROR
                )),
                // Caso: Nexi Postgres disabilitato
                Arguments.of(true, false, false, false, Map.of(
                        PAGOPA_ID, SyncStatusEnum.DONE,
                        NEXI_POSTGRES_ID, SyncStatusEnum.DISABLED
                ))
        );
    }

    private static ResponseEntity<byte[]> okResponse() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HEADER_CACHE_ID, String.valueOf(System.currentTimeMillis()));
        httpHeaders.add(HEADER_CACHE_TIMESTAMP, Instant.now().toString());
        httpHeaders.add(HEADER_CACHE_VERSION, "v1.0.0");
        return new ResponseEntity<>(new byte[0], httpHeaders, HttpStatus.OK);
    }

    /**
     * Builds the service under test. The {@link TransactionTemplate} instances are real templates
     * backed by mock transaction managers: the callback runs synchronously and begin/commit/rollback
     * are no-op, so the saga logic is exercised exactly as in production.
     */
    private ApiConfigCachePersistenceService buildService(boolean writePagoPa, boolean writeNexiPostgres) {
        ApiConfigCachePersistenceService svc = new ApiConfigCachePersistenceService(
                writePagoPa,
                writeNexiPostgres,
                3,  // saveMaxAttempts
                3,  // rollbackMaxAttempts
                pagoPACachePostgresRepository,
                nexiCachePostgresRepository,
                telemetryClient,
                new TransactionTemplate(mock(org.springframework.transaction.PlatformTransactionManager.class)),
                new TransactionTemplate(mock(org.springframework.transaction.PlatformTransactionManager.class))
        );
        ReflectionTestUtils.setField(svc, "pagopaPostgresServiceIdentifier", PAGOPA_ID);
        ReflectionTestUtils.setField(svc, "nexiPostgresServiceIdentifier", NEXI_POSTGRES_ID);
        return svc;
    }
}