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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class ApiConfigCacheService extends CommonCacheService {

    private static final String HEADER_CACHE_ID = "X-CACHE-ID";
    private static final String HEADER_CACHE_TIMESTAMP = "X-CACHE-TIMESTAMP";
    private static final String HEADER_CACHE_VERSION = "X-CACHE-VERSION";

    @Value("${api-config-cache.service.enabled}")
    private boolean enabled;
    @Value("${api-config-cache.service.subscriptionKey}")
    private String subscriptionKey;

    private final ApiConfigCacheClient apiConfigCacheClient;

    @Autowired(required = false)
    private Optional<PagoPACachePostgresRepository> pagopaPostgresRepository;
    @Autowired(required = false)
    private Optional<NexiCachePostgresRepository> nexiPostgresRepository;
    @Autowired(required = false)
    private Optional<NexiCacheOracleRepository> nexiOracleRepository;

    @Value("${app.write.cache.pagopa-postgres}")
    private Boolean pagopaPostgresWrite;
    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgresServiceIdentifier;

    @Value("${app.write.cache.nexi-postgres}")
    private Boolean nexiPostgresWrite;
    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgresServiceIdentifier;

    @Value("${app.write.cache.nexi-oracle}")
    private Boolean nexiOracleWrite;
    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    public ApiConfigCacheService(@Value("${api-config-cache.service.host}") String apiConfigCacheUrl) {
        apiConfigCacheClient = Feign.builder().target(ApiConfigCacheClient.class, apiConfigCacheUrl);
    }

    public Map<String, SyncStatusEnum> forceCacheUpdate() {
        Map<String, SyncStatusEnum> syncStatusMap = new HashMap<>();
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.config);
            }
            log.debug("SyncService api-config-cache get cache");
            Response response = apiConfigCacheClient.getCache(subscriptionKey);
            int httpResponseCode = response.status();
            if (httpResponseCode != HttpStatus.OK.value()) {
                log.error("SyncService api-config-cache get cache error - result: httpStatusCode[{}]", httpResponseCode);
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }
            log.info("SyncService api-config-cache get cache successful");

            Map<String, Collection<String>> headers = response.headers();
            if( headers.isEmpty() ) {
                log.error("SyncService api-config-cache get cache error - empty header");
                throw new AppException(AppError.INTERNAL_SERVER_ERROR);
            }
            String cacheId = (String) getHeaderParameter(TargetRefreshEnum.config, headers, HEADER_CACHE_ID);
            String cacheTimestamp = (String) getHeaderParameter(TargetRefreshEnum.config, headers, HEADER_CACHE_TIMESTAMP);
            String cacheVersion = (String) getHeaderParameter(TargetRefreshEnum.config, headers, HEADER_CACHE_VERSION);

            log.info("SyncService cacheId:[{}], cacheTimestamp:[{}], cacheVersion:[{}]", cacheId, Instant.from(ZonedDateTime.parse(cacheTimestamp)), cacheVersion);

            ConfigCache configCache = composeCache(cacheId, ZonedDateTime.parse(cacheTimestamp), cacheVersion, response.body().asInputStream().readAllBytes());

            saveAllDatabases(syncStatusMap, configCache);
        } catch (SyncDbStatusException e) {
            //viene usata per poter restituire in risposta la mappa degli aggiornamenti
            return syncStatusMap;
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService api-config-cache get cache error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("SyncService api-config-cache get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return syncStatusMap;
    }

    @Transactional(rollbackFor={SyncDbStatusException.class})
    void saveAllDatabases(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) throws SyncDbStatusException {
        savePagoPA(syncStatusMap, configCache);
        saveNexiPostgres(syncStatusMap, configCache);
        saveNexiOracle(syncStatusMap, configCache);

        if( syncStatusMap.containsValue(SyncStatusEnum.error) ) {
            syncStatusMap.forEach((k, v) -> {
                if (v == SyncStatusEnum.done) {
                    syncStatusMap.remove(k);
                    syncStatusMap.put(k, SyncStatusEnum.rollback);
                }
            });
            throw new SyncDbStatusException("Rollback sync");
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if( pagopaPostgresWrite && pagopaPostgresRepository.isPresent() ) {
                pagopaPostgresRepository.get().save(configCache);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.done);
            } else {
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.disabled);
            }
        } catch(Exception ex) {
            log.error("SyncService api-config-cache save pagoPA error: {}", ex.getMessage(), ex);
            syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.error);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if( nexiOracleWrite && nexiOracleRepository.isPresent() ) {
                nexiOracleRepository.get().save(configCache);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.done);
            } else {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.disabled);
            }
        } catch(Exception ex) {
            log.error("SyncService api-config-cache save Nexi Oracle error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.error);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if ( nexiPostgresWrite && nexiPostgresRepository.isPresent() ) {
                nexiPostgresRepository.get().save(configCache);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.done);
            } else {
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.disabled);
            }
        } catch(Exception ex) {
            log.error("SyncService api-config-cache save Nexi Postgres error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.error);
        }
    }
}
