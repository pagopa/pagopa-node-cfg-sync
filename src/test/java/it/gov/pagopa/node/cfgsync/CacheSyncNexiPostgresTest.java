package it.gov.pagopa.node.cfgsync;

import feign.Request;
import feign.Response;
import feign.mock.MockClient;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.SyncStatusResponse;
import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferences;
import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferencesView;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCdiPreferencesPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCdiPreferencesViewPostgresRepository;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import it.gov.pagopa.node.cfgsync.service.CommonCacheService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.node.cfgsync.ConstantsHelper.*;
import static it.gov.pagopa.node.cfgsync.util.Constants.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,properties = {
        "riversamento.source=nexi-postgres",
        "riversamento.target=nexi-postgres",
})
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
class CacheSyncNexiPostgresTest {

  public static final String CACHE_URL = "/ndp/cache";
  public static final String CLIENT_CACHE_PATH = "/cache";

  @Autowired private ApiConfigCacheService cacheManagerService;
  @Autowired private CommonCacheService commonCacheService;
  @Autowired private TestRestTemplate restTemplate;
  private MockClient mockClient;

  @LocalServerPort private int port;

  @Mock
  ApiConfigCacheClient apiConfigCacheClient;
  @Autowired
  private NexiCdiPreferencesViewPostgresRepository nexiCdiPreferencesViewPostgresRepository;
  @Autowired
  private NexiCdiPreferencesPostgresRepository nexiCdiPreferencesPostgresRepository;

  private static final Map<String, Collection<String>> headers;
  static {
      headers = Map.of(HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())), HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()), HEADER_CACHE_VERSION, List.of("v1.0.0"));
  }

  @Test
  void nexipostgres() throws InterruptedException {

    ReflectionTestUtils.setField(cacheManagerService, "riversamentoSource", "nexi-postgres");
    ReflectionTestUtils.setField(cacheManagerService, "riversamentoTarget", "nexi-postgres");

    long size = Math.round(Math.random()*500);
    ArrayList<CDIPreferencesView> arrayList = new ArrayList();
    for(long i = 0;i<size;i++){
      arrayList.add(new CDIPreferencesView(new Long(i),"","", BigDecimal.ZERO,new Long(i)));
    }
    nexiCdiPreferencesViewPostgresRepository.saveAll(arrayList);

    Map<String, Collection<String>> headersCustom =
            Map.of(
                    HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())),
                    HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()),
                    HEADER_CACHE_VERSION, List.of(StringUtils.repeat("*", 50))
            );
    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headersCustom)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getHeaders().isEmpty());
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    Thread.sleep(5000);
    List<CDIPreferences> all = nexiCdiPreferencesPostgresRepository.findAll();
    assertThat(all.size()).isEqualTo(size);

  }

  @Test
  void nexipostgres2() throws InterruptedException {

    ReflectionTestUtils.setField(cacheManagerService, "riversamentoEnabled", false);

    long size = Math.round(Math.random()*500);
    ArrayList<CDIPreferencesView> arrayList = new ArrayList();
    for(long i = 0;i<size;i++){
      arrayList.add(new CDIPreferencesView(new Long(i),"","", BigDecimal.ZERO,new Long(i)));
    }
    long originalcount = nexiCdiPreferencesPostgresRepository.count();
    nexiCdiPreferencesViewPostgresRepository.deleteAll();
    nexiCdiPreferencesViewPostgresRepository.saveAll(arrayList);

    Map<String, Collection<String>> headersCustom =
            Map.of(
                    HEADER_CACHE_ID, List.of(String.valueOf(System.currentTimeMillis())),
                    HEADER_CACHE_TIMESTAMP, List.of(Instant.now().toString()),
                    HEADER_CACHE_VERSION, List.of(StringUtils.repeat("*", 50))
            );
    when(apiConfigCacheClient.getCache(anyString())).thenReturn(Response
            .builder()
            .status(200)
            .reason("Mocked")
            .headers(headersCustom)
            .request(mock(Request.class))
            .body(new byte[0])
            .build());
    cacheManagerService.setApiConfigCacheClient(apiConfigCacheClient);

    ResponseEntity<List<SyncStatusResponse>> response = restTemplate.exchange(CACHE_URL, HttpMethod.PUT, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getBody()).isNotNull();
    assertFalse(response.getHeaders().isEmpty());
    assertFalse(response.getBody().isEmpty());
    assertEquals(3, response.getBody().size());
    assertThat(response.getBody().get(0).getServiceIdentifier()).isEqualTo(PAGOPAPOSTGRES_SI);
    assertThat(response.getBody().get(0).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(1).getServiceIdentifier()).isEqualTo(NEXIPOSTGRES_SI);
    assertThat(response.getBody().get(1).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    assertThat(response.getBody().get(2).getServiceIdentifier()).isEqualTo(NEXIORACLE_SI);
    assertThat(response.getBody().get(2).getStatus()).isEqualTo(SyncStatusEnum.DONE);
    Thread.sleep(5000);
    Long afterCount = nexiCdiPreferencesPostgresRepository.count();
    assertThat(afterCount).isEqualTo(originalcount);

  }

}
