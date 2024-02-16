package it.gov.pagopa.node.cfgsync.service;

import feign.Feign;
import feign.FeignException;
import feign.Response;
import it.gov.pagopa.node.cfgsync.client.ApiConfigCacheClient;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import it.gov.pagopa.node.cfgsync.exception.AppException;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.cache.NexiCacheOracleRepository;
import it.gov.pagopa.node.cfgsync.repository.nexipostgre.cache.NexiCachePostgreRepository;
import it.gov.pagopa.node.cfgsync.repository.pagopa.cache.PagoPACachePostgreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

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

    @Autowired
    private PagoPACachePostgreRepository pagoPACachePostgreRepository;
//    @Autowired
//    private NexiCachePostgreRepository nexiCachePostgreRepository;
//    @Autowired
//    private NexiCacheOracleRepository nexiCacheOracleRepository;

    @Value("${spring.datasource.pagopa.postgre.cache.enabled}")
    private Boolean pagopaPostgreCacheEnabled;

    @Value("${spring.datasource.nexi.postgre.cache.enabled}")
    private Boolean nexiPostgreCacheEnabled;

    @Value("${spring.datasource.nexi.oracle.cache.enabled}")
    private Boolean nexiOracleCacheEnabled;

    private final TransactionTemplate transactionTemplate;

    public ApiConfigCacheService(@Value("${api-config-cache.service.host}") String apiConfigCacheUrl, PlatformTransactionManager transactionManager) {
        apiConfigCacheClient = Feign.builder().target(ApiConfigCacheClient.class, apiConfigCacheUrl);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public void forceCacheUpdate() {
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

            ConfigCache configCache = composeCache(cacheId, ZonedDateTime.parse(cacheTimestamp).toLocalDateTime(), cacheVersion, response.body().asInputStream().readAllBytes());

            this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        if( pagopaPostgreCacheEnabled ) pagoPACachePostgreRepository.save(configCache);
//                        if( nexiPostgreCacheEnabled ) nexiCachePostgreRepository.save(configCache);
//                        if( nexiOracleCacheEnabled ) nexiCacheOracleRepository.save(configCache);
                    } catch(NoSuchElementException ex) {
                        status.setRollbackOnly();
                    }
                }
            });
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
    }
}
