package it.gov.pagopa.node.cfgsync.service;

import feign.Feign;
import feign.FeignException;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
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
    private Optional<PagoPACachePostgresRepository> pagoPACachePostgreRepository;
    @Autowired(required = false)
    private Optional<NexiCachePostgresRepository> nexiCachePostgreRepository;
    @Autowired(required = false)
    private Optional<NexiCacheOracleRepository> nexiCacheOracleRepository;

    @Value("${app.write.cache.pagopa-postgres}")
    private Boolean pagopaPostgresCacheEnabled;
    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgresServiceIdentifier;

    @Value("${app.write.cache.nexi-postgres}")
    private Boolean nexiPostgresCacheEnabled;
    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgresServiceIdentifier;

    @Value("${app.write.cache.nexi-oracle}")
    private Boolean nexiOracleCacheEnabled;
    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    public ApiConfigCacheService(@Value("${api-config-cache.service.host}") String apiConfigCacheUrl) {
        apiConfigCacheClient = Feign.builder().target(ApiConfigCacheClient.class, apiConfigCacheUrl);
    }

    @Transactional
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

            ConfigCache configCache = composeCache(cacheId, ZonedDateTime.parse(cacheTimestamp).toLocalDateTime(), cacheVersion, response.body().asInputStream().readAllBytes());

            try {
                if( pagopaPostgresCacheEnabled && pagoPACachePostgreRepository.isPresent() ) {
                    pagoPACachePostgreRepository.get().save(configCache);
                    syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.done);
                } else {
                    syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.disabled);
                }
            } catch(Exception ex) {
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.error);
            }
            try {
                if ( nexiPostgresCacheEnabled && nexiCachePostgreRepository.isPresent() ) {
                    nexiCachePostgreRepository.get().save(configCache);
                } else {
                    syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.disabled);
                }
            } catch(Exception ex) {
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.error);
            }
            try {
                if( nexiOracleCacheEnabled && nexiCacheOracleRepository.isPresent() ) {
                    nexiCacheOracleRepository.get().save(configCache);
                } else {
                    syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.disabled);
                }
            } catch(Exception ex) {
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.error);
            }
        } catch (FeignException.GatewayTimeout e) {
            log.error("SyncService api-config-cache get cache error: Gateway timeout", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("SyncService api-config-cache get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("SyncService api-config-cache get cache error", e);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return syncStatusMap;
    }
}
