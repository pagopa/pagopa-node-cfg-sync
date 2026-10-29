package it.gov.pagopa.node.cfgsync.service;

import feign.FeignException;
import feign.Request;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiConfigCacheFetchServiceTest {

    @Mock
    private ApiConfigCacheClient client;
    @InjectMocks
    private ApiConfigCacheFetchService service;

    @Test
    void fetchCacheWithRetry_success() throws Exception {
        when(client.getCache()).thenReturn(ResponseEntity.ok(new byte[0]));

        CompletableFuture<ResponseEntity<byte[]>> future = service.fetchCacheWithRetry();

        assertEquals(200, future.get(5, TimeUnit.SECONDS).getStatusCode().value());
    }

    @Test
    void fetchCacheWithRetry_nonOkStatus_failsWithAppException() {
        when(client.getCache()).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        CompletableFuture<ResponseEntity<byte[]>> future = service.fetchCacheWithRetry();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        AppException appException = assertInstanceOf(AppException.class, ex.getCause());
        assertEquals(AppError.CACHE_UNPROCESSABLE.httpStatus, appException.getHttpStatus());
    }

    @Test
    void fetchCacheWithRetry_clientThrows_failsWithAppException() {
        Response failResponse = Response.builder().status(503).request(mock(Request.class)).build();
        FeignException feign503 = FeignException.errorStatus("getCache", failResponse);
        when(client.getCache()).thenThrow(feign503);

        CompletableFuture<ResponseEntity<byte[]>> future = service.fetchCacheWithRetry();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        AppException appException = assertInstanceOf(AppException.class, ex.getCause());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, appException.getHttpStatus());
    }
}