package it.gov.pagopa.node.cfgsync.service;

import feign.Response;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_ID;
import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_TIMESTAMP;
import static it.gov.pagopa.node.cfgsync.util.Constants.HEADER_CACHE_VERSION;

@Service
@Slf4j
public class ApiConfigCachePersistenceService extends CommonCacheService {

    private final boolean apiConfigCacheWritePagoPa;
    private final boolean apiConfigCacheWriteNexiOracle;
    private final boolean apiConfigCacheWriteNexiPostgres;
    private final PagoPACachePostgresRepository pagoPACachePostgresRepository;
    private final NexiCachePostgresRepository nexiCachePostgresRepository;
    private final NexiCacheOracleRepository nexiCacheOracleRepository;

    public ApiConfigCachePersistenceService(
            @Value("${api-config-cache.write.pagopa-postgres}") boolean apiConfigCacheWritePagoPa,
            @Value("${api-config-cache.write.nexi-oracle}") boolean apiConfigCacheWriteNexiOracle,
            @Value("${api-config-cache.write.nexi-postgres}") boolean apiConfigCacheWriteNexiPostgres,
            @Autowired(required = false) PagoPACachePostgresRepository pagoPACachePostgresRepository,
            @Autowired(required = false) NexiCachePostgresRepository nexiCachePostgresRepository,
            @Autowired(required = false) NexiCacheOracleRepository nexiCacheOracleRepository
    ) {
        this.apiConfigCacheWritePagoPa = apiConfigCacheWritePagoPa;
        this.apiConfigCacheWriteNexiOracle = apiConfigCacheWriteNexiOracle;
        this.apiConfigCacheWriteNexiPostgres = apiConfigCacheWriteNexiPostgres;
        this.pagoPACachePostgresRepository = pagoPACachePostgresRepository;
        this.nexiCachePostgresRepository = nexiCachePostgresRepository;
        this.nexiCacheOracleRepository = nexiCacheOracleRepository;
    }

    @Transactional(rollbackFor = {SyncDbStatusException.class})
    public Map<String, SyncStatusEnum> saveCache(Response response) {
        Map<String, SyncStatusEnum> syncStatusMap = new LinkedHashMap<>();

        try {
            Map<String, Collection<String>> headers = response.headers();
            if (headers.isEmpty()) {
                throw new AppException(AppError.INTERNAL_SERVER_ERROR, "empty headers");
            }

            String cacheId = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_ID);
            String cacheTimestamp = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_TIMESTAMP);
            String cacheVersion = (String) getHeaderParameter(TargetRefreshEnum.cache.label, headers, HEADER_CACHE_VERSION);

            ConfigCache configCache = composeCache(
                    cacheId,
                    ZonedDateTime.parse(cacheTimestamp),
                    cacheVersion,
                    response.body().asInputStream().readAllBytes()
            );

            savePagoPA(syncStatusMap, configCache);
            saveNexiPostgres(syncStatusMap, configCache);
            saveNexiOracle(syncStatusMap, configCache);

            return composeSyncStatusMapResult(TargetRefreshEnum.cache.label, syncStatusMap);

        } catch (Exception e) {
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        }
    }

    private void savePagoPA(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        if (apiConfigCacheWritePagoPa) {
            try {
                pagoPACachePostgresRepository.save(configCache);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } catch (Exception ex) {
                log.error("[{}][ALERT] Problem to dump cache on PagoPA PostgreSQL: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
                syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.ERROR);
            }
        } else {
            syncStatusMap.put(pagopaPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
        }
    }

    private void saveNexiOracle(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        if (apiConfigCacheWriteNexiOracle) {
            try {
                nexiCacheOracleRepository.save(configCache);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DONE);
            } catch (Exception ex) {
                log.error("[{}][ALERT] Problem to dump cache on Nexi Oracle: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
                syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.ERROR);
            }
        } else {
            syncStatusMap.put(nexiOracleServiceIdentifier, SyncStatusEnum.DISABLED);
        }
    }

    private void saveNexiPostgres(Map<String, SyncStatusEnum> syncStatusMap, ConfigCache configCache) {
        if (apiConfigCacheWriteNexiPostgres) {
            try {
                nexiCachePostgresRepository.save(configCache);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DONE);
            } catch (Exception ex) {
                log.error("[{}][ALERT] Problem to dump cache on Nexi PostgreSQL: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
                syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.ERROR);
            }
        } else {
            syncStatusMap.put(nexiPostgresServiceIdentifier, SyncStatusEnum.DISABLED);
        }
    }
}
