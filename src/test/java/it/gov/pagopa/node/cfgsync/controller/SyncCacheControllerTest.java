package it.gov.pagopa.node.cfgsync.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.gov.pagopa.node.cfgsync.config.ViewNamingStrategy;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SyncCacheController.class)
class SyncCacheControllerTest {

  private static final String CACHE_URL = "/ndp/cache";
  private static final String CUSTOM_EVENT_URL = "/ndp/custom-event";
  private static final String PAGOPAPOSTGRES_SI = "PAGOPAPOSTGRES";
  private static final String NEXIPOSTGRES_SI = "NEXIPOSTGRES";

  @Autowired private MockMvc mockMvc;

  @MockBean private ApiConfigCacheService apiConfigCacheService;

  @MockBean private StandInManagerService standInManagerService;

  @MockBean private ViewNamingStrategy viewNamingStrategy;

  @Test
  void cache_success() throws Exception {
    Map<String, SyncStatusEnum> result = new LinkedHashMap<>();
    result.put(PAGOPAPOSTGRES_SI, SyncStatusEnum.DONE);
    result.put(NEXIPOSTGRES_SI, SyncStatusEnum.DISABLED);
    when(apiConfigCacheService.syncCache()).thenReturn(result);

    mockMvc
        .perform(put(CACHE_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$[0].serviceIdentifier").value(PAGOPAPOSTGRES_SI))
        .andExpect(jsonPath("$[0].status").value(SyncStatusEnum.DONE.name()))
        .andExpect(jsonPath("$[1].serviceIdentifier").value(NEXIPOSTGRES_SI))
        .andExpect(jsonPath("$[1].status").value(SyncStatusEnum.DISABLED.name()));

    verify(apiConfigCacheService).syncCache();
  }

  @Test
  void cache_serviceDisabled_returns400() throws Exception {
    when(apiConfigCacheService.syncCache())
        .thenThrow(new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.cache.label));

    mockMvc
        .perform(put(CACHE_URL))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value(AppError.SERVICE_DISABLED.title));
  }

  @Test
  void cache_unprocessable_returns422() throws Exception {
    when(apiConfigCacheService.syncCache())
        .thenThrow(new AppException(AppError.CACHE_UNPROCESSABLE));

    mockMvc
        .perform(put(CACHE_URL))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.title").value(AppError.CACHE_UNPROCESSABLE.title));
  }

  @Test
  void cache_genericException_returns500() throws Exception {
    when(apiConfigCacheService.syncCache()).thenThrow(new RuntimeException("boom"));

    mockMvc
        .perform(put(CACHE_URL))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500));
  }

  @Test
  void customEvent_success() throws Exception {
    doNothing().when(apiConfigCacheService).customEvent();

    mockMvc.perform(get(CUSTOM_EVENT_URL)).andExpect(status().isOk());

    verify(apiConfigCacheService).customEvent();
  }
}
