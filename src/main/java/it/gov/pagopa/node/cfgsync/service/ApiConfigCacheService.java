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
import it.gov.pagopa.node.cfgsync.repository.nexioracle.NexiStandInOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiCachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.NexiStandInPostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPACachePostgresRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

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
    private boolean enabled;

    @Value("${api-config-cache.service.subscriptionKey}")
    private String subscriptionKey;

    @Value("${api-config-cache.service.host}")
    private String apiConfigCacheUrl;

    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    @Value("${api-config-cache.write.pagopa-postgres}")
    private boolean writePagoPa;

    @Value("${api-config-cache.write.nexi-oracle}")
    private boolean writeNexiOracle;

    @Value("${api-config-cache.write.nexi-postgres}")
    private boolean writeNexiPostgres;

    private ApiConfigCacheClient apiConfigCacheClient;

    @Autowired(required = false)
    private PagoPACachePostgresRepository pagopaPostgresRepository;
    @Autowired(required = false)
    private NexiCachePostgresRepository nexiPostgresRepository;
    @Autowired(required = false)
    private NexiCacheOracleRepository nexiOracleRepository;

    @PostMapping
    private void setStandInManagerClient() {
        apiConfigCacheClient = Feign.builder().target(ApiConfigCacheClient.class, apiConfigCacheUrl);
    }

    @Transactional(rollbackFor={SyncDbStatusException.class})
    public Map<String, SyncStatusEnum> forceCacheUpdate() {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();
        try {
            if( !enabled ) {
                throw new AppException(AppError.SERVICE_DISABLED, TargetRefreshEnum.cache.label);
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
            String cacheId = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_ID);
            String cacheTimestamp = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_TIMESTAMP);
            String cacheVersion = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_VERSION);

            log.info("SyncService cacheId:[{}], cacheTimestamp:[{}], cacheVersion:[{}]", cacheId, Instant.from(ZonedDateTime.parse(cacheTimestamp)), cacheVersion);

            ConfigCache configCache = composeCache(cacheId, ZonedDateTime.parse(cacheTimestamp), cacheVersion, response.body().asInputStream().readAllBytes());

            savePagoPA(syncStatusMap, configCache);
            saveNexiPostgres(syncStatusMap, configCache);
            saveNexiOracle(syncStatusMap, configCache);

            Map<String, SyncStatusEnum> syncStatusMapUpdated = new LinkedHashMap<>();
            if( syncStatusMap.containsValue(SyncStatusEnum.ERROR) ) {
                syncStatusMap.forEach((k, v) -> {
                    if (v == SyncStatusEnum.DONE) {
                        syncStatusMapUpdated.put(k, SyncStatusEnum.ROLLBACK);
                    } else {
                        syncStatusMapUpdated.put(k, v);
                    }
                });
                return syncStatusMapUpdated;
            } else {
                return syncStatusMap;
            }
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService api-config-cache get cache error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch(AppException appException) {
            throw appException;
        } catch (Exception e) {
            log.error("SyncService api-config-cache get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if( writePagoPa ) {
                pagopaPostgresRepository.save(configCache);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService api-config-cache save pagoPA error: {}", ex.getMessage(), ex);
            syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if( writeNexiOracle ) {
                nexiOracleRepository.save(configCache);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService api-config-cache save Nexi Oracle error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        try {
            if ( writeNexiPostgres ) {
                nexiPostgresRepository.save(configCache);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } else {
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
            }
        } catch(Exception ex) {
            log.error("SyncService api-config-cache save Nexi Postgres error: {}", ex.getMessage(), ex);
            syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.ERROR);
        }
    }
}
