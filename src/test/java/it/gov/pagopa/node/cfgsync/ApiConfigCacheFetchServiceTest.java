package it.gov.pagopa.node.cfgsync;

import feign.Response;
import feign.FeignException;
import feign.Request;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheFetchService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiConfigCacheFetchServiceTest {

    private ApiConfigCacheFetchService service;
    private ApiConfigCacheClient client;

    @BeforeEach
    void setUp() {
        service = new ApiConfigCacheFetchService();
        client = mock(ApiConfigCacheClient.class);
        service.setApiConfigCacheClient(client);
        service.setApiConfigCacheSubscriptionKey("key");
        service.setRetryLeft(2);
        service.setAttempt(1);
        // Use real scheduler from production code (do not inject test scheduler)
    }

    @Test
    void fetchCacheWithRetry_success() {
        Response response = Response.builder().status(200).request(mock(Request.class)).build();
        when(client.getCache(anyString())).thenReturn(response);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertEquals(200, future.get(5, TimeUnit.SECONDS).status());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchCacheWithRetry_failure() {
        Response response = Response.builder().status(500).request(mock(Request.class)).build();
        when(client.getCache(anyString())).thenReturn(response);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertThrows(java.util.concurrent.ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            // if timeout or interrupted
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_shouldRetryAndSucceed() {
        Response failResponse = Response.builder().status(500).request(mock(Request.class)).build();
        Response successResponse = Response.builder().status(200).request(mock(Request.class)).build();
        when(client.getCache(anyString()))
                .thenReturn(failResponse)
                .thenReturn(successResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertEquals(200, future.get(10, TimeUnit.SECONDS).status());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_shouldFailAfterRetries() {
        Response failResponse = Response.builder().status(500).request(mock(Request.class)).build();
        when(client.getCache(anyString())).thenReturn(failResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertThrows(java.util.concurrent.ExecutionException.class, () -> future.get(15, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // New tests

    @Test
    void fetchWithRetry_retryableException_thenSuccess() {
        Response successResponse = Response.builder().status(200).request(mock(Request.class)).build();
        Response failResponse = Response.builder().status(503).request(mock(Request.class)).build();
        FeignException feign503 = FeignException.errorStatus("getCache", failResponse);
        when(client.getCache(anyString()))
                .thenThrow(feign503)
                .thenReturn(successResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertEquals(200, future.get(10, TimeUnit.SECONDS).status());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_ioException_thenSuccess() {
        Response successResponse = Response.builder().status(200).request(mock(Request.class)).build();
        CompletionException wrappedIo = new CompletionException(new IOException("io"));
        when(client.getCache(anyString()))
                .thenThrow(wrappedIo)
                .thenReturn(successResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertEquals(200, future.get(10, TimeUnit.SECONDS).status());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_feign500_thenSuccess() {
        Response failResponse = Response.builder().status(500).request(mock(Request.class)).build();
        Response successResponse = Response.builder().status(200).request(mock(Request.class)).build();
        FeignException feign500 = FeignException.errorStatus("getCache", failResponse);
        when(client.getCache(anyString()))
                .thenThrow(feign500)
                .thenReturn(successResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertEquals(200, future.get(10, TimeUnit.SECONDS).status());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_feign404_noRetry() {
        Response notFound = Response.builder().status(404).request(mock(Request.class)).build();
        FeignException feign404 = FeignException.errorStatus("getCache", notFound);
        when(client.getCache(anyString())).thenThrow(feign404);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertThrows(java.util.concurrent.ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_runtimeException_noRetry() {
        RuntimeException re = new RuntimeException("boom");
        when(client.getCache(anyString())).thenThrow(re);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertThrows(java.util.concurrent.ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchWithRetry_completionException_unwrapped() {
        Response successResponse = Response.builder().status(200).request(mock(Request.class)).build();
        Response failResponse = Response.builder().status(503).request(mock(Request.class)).build();
        FeignException feign503 = FeignException.errorStatus("getCache", failResponse);
        CompletionException wrapped = new CompletionException(feign503);
        when(client.getCache(anyString()))
                .thenThrow(wrapped)
                .thenReturn(successResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        try {
            assertEquals(200, future.get(10, TimeUnit.SECONDS).status());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}