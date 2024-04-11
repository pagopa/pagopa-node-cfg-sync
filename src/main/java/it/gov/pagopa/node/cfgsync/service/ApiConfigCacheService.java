package it.gov.pagopa.node.cfgsync.service;

import feign.Feign;
import feign.FeignException;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiCacheOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPACachePostgresRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static it.gov.pagopa.node.cfgsync.util.Constants.*;

@Service
@Setter
@Slf4j
@RequiredArgsConstructor
public class ApiConfigCacheService extends CommonCacheService {

    @Value("${api-config-cache.service.enabled}")
    private boolean apiConfigCacheServiceEnabled;

    @Value("${api-config-cache.service.subscriptionKey}")
    private String apiConfigCacheSubscriptionKey;

    @Value("${api-config-cache.service.host}")
    private String apiConfigCacheUrl;

    @Value("${api-config-cache.write.pagopa-postgres}")
    private boolean apiConfigCacheWritePagoPa;

    @Value("${api-config-cache.write.nexi-oracle}")
    private boolean apiConfigCacheWriteNexiOracle;

    @Value("${api-config-cache.write.nexi-postgres}")
    private boolean apiConfigCacheWriteNexiPostgres;

    private ApiConfigCacheClient apiConfigCacheClient;

    @Autowired(required = false)
    private PagoPACachePostgresRepository pagoPACachePostgresRepository;
    @Autowired(required = false)
    private NexiCachePostgresRepository nexiCachePostgresRepository;
    @Autowired(required = false)
    private NexiCacheOracleRepository nexiCacheOracleRepository;

    @PostConstruct
    private void setStandInManagerClient() {
        apiConfigCacheClient = Feign.builder().target(ApiConfigCacheClient.class, apiConfigCacheUrl);
    }

    @Transactional(rollbackFor={SyncDbStatusException.class})
    public Map<String, SyncStatusEnum> syncCache() {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();
        try {
            if( !apiConfigCacheServiceEnabled) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.cache.label);
            }
            Response response = apiConfigCacheClient.getCache(apiConfigCacheSubscriptionKey);
            int httpResponseCode = response.status();
            if (httpResponseCode != HttpStatus.OK.value()) {
                log.error("[{}] error - result: httpStatusCode[{}]", TargetRefreshEnum.cache.label, httpResponseCode);
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }

            Map<String, Collection<String>> headers = response.headers();
            if( headers.isEmpty() ) {
                log.error("[{}] response error - empty header", TargetRefreshEnum.cache.label);
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }
            String cacheId = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_ID);
            String cacheTimestamp = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_TIMESTAMP);
            String cacheVersion = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_VERSION);

            log.info("[{}] response successful. cacheId:[{}], cacheTimestamp:[{}], cacheVersion:[{}]", TargetRefreshEnum.cache.label, cacheId, Instant.from(ZonedDateTime.parse(cacheTimestamp)), cacheVersion);

            ConfigCache configCache = composeCache(cacheId, ZonedDateTime.parse(cacheTimestamp), cacheVersion, response.body().asInputStream().readAllBytes());

            savePagoPA(syncStatusMap, configCache);
            saveNexiPostgres(syncStatusMap, configCache);
            saveNexiOracle(syncStatusMap, configCache);

            return composeSyncStatusMapResult(TargetRefreshEnum.cache.label, syncStatusMap);
        } catch (FeignException fEx) {
            log.error("[{}] error: {}", TargetRefreshEnum.cache.label, fEx.getMessage(), fEx);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch(AppException appException) {
            throw appException;
        } catch (Exception ex) {
            log.error("[{}][ALERT] Generic Error: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if(apiConfigCacheWritePagoPa) {
                pagoPACachePostgresRepository.save(configCache);
                syncStatusMap.put(getPagopaPostgresServiceIdentifier(), SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(getPagopaPostgresServiceIdentifier(), SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("[{}][ALERT] Problem to dump cache on PagoPA PostgreSQL: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
            syncStatusMap.put(getPagopaPostgresServiceIdentifier(), SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if(apiConfigCacheWriteNexiOracle) {
                nexiCacheOracleRepository.save(configCache);
                syncStatusMap.put(getNexiOracleServiceIdentifier(), SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(getNexiOracleServiceIdentifier(), SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("[{}][ALERT] Problem to dump cache on Nexi Oracle: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
            syncStatusMap.put(getNexiOracleServiceIdentifier(), SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if (apiConfigCacheWriteNexiPostgres) {
                nexiCachePostgresRepository.save(configCache);
                syncStatusMap.put(getNexiPostgresServiceIdentifier(), SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(getNexiPostgresServiceIdentifier(), SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("[{}][ALERT] Problem to dump cache on Nexi PostgreSQL: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
            syncStatusMap.put(getNexiPostgresServiceIdentifier(), SyncStatusEnum.ERROR);
        }
    }
}
