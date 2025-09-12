package it.gov.pagopa.node.cfgsync;

import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheFetchService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
    }

    @Test
    void fetchCacheWithRetry_success() {
        Response response = Response.builder().status(200).request(mock(feign.Request.class)).build();
        when(client.getCache(anyString())).thenReturn(response);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        assertEquals(200, future.join().status());
    }

    @Test
    void fetchCacheWithRetry_failure() {
        Response response = Response.builder().status(500).request(mock(feign.Request.class)).build();
        when(client.getCache(anyString())).thenReturn(response);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        CompletionException ex = assertThrows(CompletionException.class, future::join);
        Assertions.assertInstanceOf(AppException.class, ex.getCause());
    }

    // Java
    @Test
    void fetchWithRetry_shouldReturnResponseOnFirstTry() {
        Response response = Response.builder().status(200).request(mock(feign.Request.class)).build();
        when(client.getCache(anyString())).thenReturn(response);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        assertEquals(200, future.join().status());
    }

    @Test
    void fetchWithRetry_shouldRetryAndSucceed() {
        Response failResponse = Response.builder().status(500).request(mock(feign.Request.class)).build();
        Response successResponse = Response.builder().status(200).request(mock(feign.Request.class)).build();
        when(client.getCache(anyString()))
                .thenReturn(failResponse)
                .thenReturn(successResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        assertEquals(200, future.join().status());
    }

    @Test
    void fetchWithRetry_shouldFailAfterRetries() {
        Response failResponse = Response.builder().status(500).request(mock(feign.Request.class)).build();
        when(client.getCache(anyString())).thenReturn(failResponse);

        CompletableFuture<Response> future = service.fetchCacheWithRetry();
        CompletionException ex = assertThrows(CompletionException.class, future::join);
        Assertions.assertInstanceOf(AppException.class, ex.getCause());
    }
}