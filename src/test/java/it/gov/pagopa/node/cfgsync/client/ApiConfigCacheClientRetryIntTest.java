package it.gov.pagopa.node.cfgsync.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheFetchService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test for the annotation-driven retry ({@link
 * org.springframework.retry.annotation.Retryable}) applied on {@link
 * ApiConfigCacheClient#getCache()}.
 *
 * <p>{@code getCache()} returns a {@link org.springframework.http.ResponseEntity}: this makes Feign
 * go through its error handling, throwing a {@code FeignException.FeignServerException} on 5xx
 * (retried) and a {@code FeignException.FeignClientException} on 4xx (excluded from retry).
 * Transport-level failures (connection reset, timeouts, ...) also throw and are retried; here they
 * are simulated with WireMock {@link Fault}s.
 */
@SpringBootTest(
    classes = ApiConfigCacheClientRetryIntTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "api-config-cache.service.subscriptionKey=test-subkey",
      // maxAttemptsExpression -> total attempts (1 initial + retries)
      "api-config-cache.service.retryLeft=3",
      "api-config-cache.service.attemptDelay=20"
    })
class ApiConfigCacheClientRetryIntTest {

  private static final String CACHE_PATH = "/stakeholders/node/cache/schemas/v1";
  private static final int EXPECTED_MAX_ATTEMPTS = 3; // == api-config-cache.service.retryLeft

  /**
   * Minimal context: only the Feign infrastructure, the Spring Retry aspect and the service under
   * test.
   */
  @EnableRetry
  @EnableFeignClients(clients = ApiConfigCacheClient.class)
  @ImportAutoConfiguration({
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    FeignAutoConfiguration.class
  })
  @Import(ApiConfigCacheFetchService.class)
  @Configuration
  static class TestConfig {}

  private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

  @DynamicPropertySource
  static void overrideBaseUrl(DynamicPropertyRegistry registry) {
    if (!WIRE_MOCK.isRunning()) {
      WIRE_MOCK.start();
    }
    registry.add("api-config-cache.service.base-url", () -> "http://localhost:" + WIRE_MOCK.port());
  }

  @AfterAll
  static void stopWireMock() {
    WIRE_MOCK.stop();
  }

  @Autowired private ApiConfigCacheFetchService service;

  @BeforeEach
  void resetWireMock() {
    WIRE_MOCK.resetAll();
  }

  @Test
  void shouldRetryOnServerErrorThenSucceed() throws Exception {
    // 1st call -> 503 (FeignServerException, retryable), 2nd call -> 200
    WIRE_MOCK.stubFor(
        get(urlEqualTo(CACHE_PATH))
            .inScenario("retry-then-ok")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("failed-once"));
    WIRE_MOCK.stubFor(
        get(urlEqualTo(CACHE_PATH))
            .inScenario("retry-then-ok")
            .whenScenarioStateIs("failed-once")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"cache\":\"ok\"}")));

    ResponseEntity<byte[]> response = service.fetchCacheWithRetry().get(10, TimeUnit.SECONDS);

    assertEquals(200, response.getStatusCode().value());
    // exactly 2 requests: 1 failure + 1 success
    WIRE_MOCK.verify(2, getRequestedFor(urlEqualTo(CACHE_PATH)));
    // subscription key header is propagated by the Feign request interceptor
    WIRE_MOCK.verify(
        getRequestedFor(urlEqualTo(CACHE_PATH))
            .withHeader("Ocp-Apim-Subscription-Key", equalTo("test-subkey")));
  }

  @Test
  void shouldRetryOnTransientFaultThenSucceed() throws Exception {
    // 1st call -> connection reset (retryable transport error), 2nd call -> 200
    WIRE_MOCK.stubFor(
        get(urlEqualTo(CACHE_PATH))
            .inScenario("retry-then-ok")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
            .willSetStateTo("failed-once"));
    WIRE_MOCK.stubFor(
        get(urlEqualTo(CACHE_PATH))
            .inScenario("retry-then-ok")
            .whenScenarioStateIs("failed-once")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"cache\":\"ok\"}")));

    ResponseEntity<byte[]> response = service.fetchCacheWithRetry().get(10, TimeUnit.SECONDS);

    assertEquals(200, response.getStatusCode().value());
    // exactly 2 requests: 1 transient failure + 1 success
    WIRE_MOCK.verify(2, getRequestedFor(urlEqualTo(CACHE_PATH)));
  }

  @Test
  void shouldExhaustRetriesOnPersistentServerError() {
    WIRE_MOCK.stubFor(get(urlEqualTo(CACHE_PATH)).willReturn(aResponse().withStatus(503)));

    ExecutionException ex =
        assertThrows(
            ExecutionException.class,
            () -> service.fetchCacheWithRetry().get(10, TimeUnit.SECONDS));
    assertInstanceOf(AppException.class, ex.getCause());

    // the server is hit exactly maxAttempts times
    WIRE_MOCK.verify(EXPECTED_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(CACHE_PATH)));
  }

  @Test
  void shouldNotRetryOnClientError() {
    // 4xx -> FeignClientException is in the @Retryable 'exclude' list -> no retry, single request
    WIRE_MOCK.stubFor(get(urlEqualTo(CACHE_PATH)).willReturn(aResponse().withStatus(404)));

    ExecutionException ex =
        assertThrows(
            ExecutionException.class,
            () -> service.fetchCacheWithRetry().get(10, TimeUnit.SECONDS));
    assertInstanceOf(AppException.class, ex.getCause());

    WIRE_MOCK.verify(1, getRequestedFor(urlEqualTo(CACHE_PATH)));
  }
}
