package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;
import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.util.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class CommonCacheService {

    @Value("${app.identifiers.pagopa-postgres}")
    private String pagopaPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-postgres}")
    private String nexiPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-oracle}")
    private String nexiOracleServiceIdentifier;

    protected ConfigCache composeCache(String cacheId, ZonedDateTime timestamp, String cacheVersion, byte[] cache) throws IOException, SyncDbStatusException {
        String version = (String) Utils.trimValueColumn(ConfigCache.class, "version", cacheVersion);
        return new ConfigCache(cacheId, timestamp, Utils.zipContent(cache), version);
    }

    protected Object getHeaderParameter(String target, Map<String, Collection<String>> headers, String key) {
        List<String> valueList = headers.get(key).stream().toList();
        if(valueList.isEmpty()) {
            log.error("[NODE-CFG-SYNC] {} get cache error - empty parameter '{}'", target, key);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return valueList.get(0);
    }

    protected Map<String, SyncStatusEnum> composeSyncStatusMapResult(String event, Map<String, SyncStatusEnum> syncStatusMap) {
        Map<String, SyncStatusEnum> syncStatusMapUpdated = new LinkedHashMap<>();
        if( syncStatusMap.containsValue(SyncStatusEnum.ERROR) ) {
            syncStatusMap.forEach((k, v) -> {
                if (v == SyncStatusEnum.DONE) {
                    syncStatusMapUpdated.put(k, SyncStatusEnum.ROLLBACK);
                } else {
                    syncStatusMapUpdated.put(k, v);
                }
            });
            log.info("[{}] Processed event: {}", event, syncStatusMapUpdated);
            return syncStatusMapUpdated;
        } else {
            log.info("[{}][ALERT-OK] Processed event: {}", event, syncStatusMap);
            return syncStatusMap;
        }
    }

}
