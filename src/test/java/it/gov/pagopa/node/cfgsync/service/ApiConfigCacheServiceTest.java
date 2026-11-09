package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiConfigCacheServiceTest {

    private ApiConfigCacheFetchService fetchService;
    private ApiConfigCachePersistenceService persistenceService;
    private ApiConfigCacheService service;
    private AppInsightTelemetryClient telemetryClient;

    @BeforeEach
    void setUp() {
        fetchService = mock(ApiConfigCacheFetchService.class);
        persistenceService = mock(ApiConfigCachePersistenceService.class);
        telemetryClient = mock(AppInsightTelemetryClient.class);
        service = new ApiConfigCacheService(fetchService, persistenceService, telemetryClient);
        service.setApiConfigCacheServiceEnabled(true);
    }

    @Test
    void syncCache_serviceDisabled_throwsException() {
        service.setApiConfigCacheServiceEnabled(false);
        AppException ex = assertThrows(AppException.class, service::syncCache);
        assertEquals(AppError.SERVICE_DISABLED.httpStatus, ex.getHttpStatus());
        assertEquals(AppError.SERVICE_DISABLED.title, ex.getTitle());
    }

    @Test
    void syncCache_success_returnsResponse() {
        Map<String, SyncStatusEnum> expected = Map.of("NDP001TEST", SyncStatusEnum.DONE);

        ResponseEntity<byte[]> response = ResponseEntity.ok(new byte[0]);

        when(fetchService.fetchCacheWithRetry()).thenReturn(CompletableFuture.completedFuture(response));
        when(persistenceService.saveCache(any())).thenReturn(expected);

        Map<String, SyncStatusEnum> result = service.syncCache();
        assertEquals(expected, result);
    }

    @Test
    void syncCache_fetchThrows_throwsAppException() {
        when(fetchService.fetchCacheWithRetry()).thenReturn(
                CompletableFuture.failedFuture(new ExecutionException("fail", null))
        );
        AppException ex = assertThrows(AppException.class, service::syncCache);
        assertEquals(AppError.CACHE_UNPROCESSABLE.httpStatus, ex.getHttpStatus());
        assertEquals(AppError.CACHE_UNPROCESSABLE.title, ex.getTitle());
        verify(telemetryClient).createCustomEventForAlert(eq(AppError.CACHE_UNPROCESSABLE), anyString(), any(ExecutionException.class));
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void syncCache_saveThrowsAppException_propagatesOriginal() {
        ResponseEntity<byte[]> response = ResponseEntity.ok(new byte[0]);
        AppException original = new AppException(AppError.INTERNAL_SERVER_ERROR, "empty headers");

        when(fetchService.fetchCacheWithRetry()).thenReturn(CompletableFuture.completedFuture(response));
        when(persistenceService.saveCache(any())).thenThrow(original);

        AppException ex = assertThrows(AppException.class, service::syncCache);
        assertSame(original, ex);
        verify(telemetryClient).createCustomEventForAlert(eq(AppError.CACHE_UNPROCESSABLE), anyString(), any(ExecutionException.class));
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void syncCache_interrupted_restoresInterruptFlag() {
        // never-completing future: get() throws InterruptedException because the flag is already set
        when(fetchService.fetchCacheWithRetry()).thenReturn(new CompletableFuture<>());
        Thread.currentThread().interrupt();
        try {
            AppException ex = assertThrows(AppException.class, service::syncCache);
            assertEquals(AppError.CACHE_UNPROCESSABLE.title, ex.getTitle());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(telemetryClient).createCustomEventForAlert(eq(AppError.CACHE_UNPROCESSABLE), anyString(), any(InterruptedException.class));
        } finally {
            Thread.interrupted(); // clear flag for other tests
        }
    }
}