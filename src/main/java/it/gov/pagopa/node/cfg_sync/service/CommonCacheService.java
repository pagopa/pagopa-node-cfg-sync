package it.gov.pagopa.node.cfg_sync.service;

import it.gov.pagopa.node.cfg_sync.exception.AppError;
import it.gov.pagopa.node.cfg_sync.exception.AppException;
import it.gov.pagopa.node.cfg_sync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfg_sync.repository.model.Cache;
import it.gov.pagopa.node.cfg_sync.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class CommonCacheService {

    @Value("${app.trimCacheColumn}")
    private boolean trimCacheColumn;

    protected Cache composeCache(String cacheId, LocalDateTime timestamp, String cacheVersion, byte[] cache) throws IOException {
        String version = trimCacheColumn ?
                (String) Utils.trimValueColumn(Cache.class, "version", cacheVersion) : cacheVersion;

        return Cache
                .builder()
                .id(cacheId)
                .time(timestamp)
                .version(version)
                .cache(Utils.zipContent(cache)).build();
    }

    protected Object getHeaderParameter(TargetRefreshEnum target, Map<String, Collection<String>> headers, String key) {
        List<String> valueList = headers.get(key).stream().toList();
        if(valueList.isEmpty()) {
            log.error("SyncService {} get cache error - empty parameter '{}'", target, key);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        return valueList.get(0);
    }

}
