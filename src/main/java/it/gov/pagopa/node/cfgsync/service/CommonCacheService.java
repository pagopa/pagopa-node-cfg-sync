package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.util.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Component
@RequiredArgsConstructor
public class CommonCacheService {

    @Value("${app.identifiers.pagopa-postgres}")
    protected String pagopaPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-postgres}")
    protected String nexiPostgresServiceIdentifier;

    @Value("${app.identifiers.nexi-oracle}")
    protected String nexiOracleServiceIdentifier;

    @Value("${app.trimCacheColumn}")
    private boolean trimCacheColumn;

    protected ConfigCache composeCache(String cacheId, ZonedDateTime timestamp, String cacheVersion, byte[] cache) throws IOException, SyncDbStatusException {
        String version = trimCacheColumn ?
                (String) Utils.trimValueColumn(ConfigCache.class, "version", cacheVersion) : cacheVersion;

        return new ConfigCache(cacheId, timestamp, Utils.zipContent(cache), version);
    }

    protected Object getHeaderParameter(String target, Map<String, Collection<String>> headers, String key) {
        List<String> valueList = headers.get(key).stream().toList();
        if(valueList.isEmpty()) {
            log.error("SyncService {} get cache error - empty parameter '{}'", target, key);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return valueList.get(0);
    }

}
