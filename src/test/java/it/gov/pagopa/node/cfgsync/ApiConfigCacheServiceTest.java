package it.gov.pagopa.node.cfgsync;

import feign.Request;
import feign.Response;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheFetchService;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCachePersistenceService;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static it.gov.pagopa.node.cfgsync.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiConfigCacheServiceTest {

    private ApiConfigCacheFetchService fetchService;
    private ApiConfigCachePersistenceService persistenceService;
    private ApiConfigCacheService service;

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
        fetchService = mock(ApiConfigCacheFetchService.class);
        persistenceService = mock(ApiConfigCachePersistenceService.class);
        service = new ApiConfigCacheService(fetchService, persistenceService);
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

        Response response = Response.builder()
                .status(200)
                .reason("Mocked")
                .headers(headers)
                .request(mock(Request.class))
                .body(new byte[0])
                .build();

        when(fetchService.fetchCacheWithRetry()) .thenReturn(CompletableFuture.completedFuture(response));
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
    }
}