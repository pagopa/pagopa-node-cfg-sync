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
import it.gov.pagopa.node.cfgsync.repository.model.*;
import it.gov.pagopa.node.cfgsync.repository.nexioracle.*;
import it.gov.pagopa.node.cfgsync.repository.nexipostgres.*;
import it.gov.pagopa.node.cfgsync.repository.pagopa.*;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Value("${riversamento.enabled}")
    private boolean riversamentoEnabled;

    @Value("${riversamento.source}")
    private String riversamentoSource;

    @Value("${riversamento.target}")
    private String riversamentoTarget;

    private ApiConfigCacheClient apiConfigCacheClient;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired(required = false)
    private PagoPACachePostgresRepository pagoPACachePostgresRepository;
    @Autowired(required = false)
    private NexiCachePostgresRepository nexiCachePostgresRepository;
    @Autowired(required = false)
    private NexiCacheOracleRepository nexiCacheOracleRepository;

    @Autowired(required = false)
    private PagoPaElencoServiziPostgresRepository pagoPaElencoServiziPostgresRepository;
    @Autowired(required = false)
    private PagoPaElencoServiziViewPostgresRepository pagoPaElencoServiziViewPostgresRepository;
    @Autowired(required = false)
    private NexiElencoServiziOracleRepository nexiElencoServiziOracleRepository;
    @Autowired(required = false)
    private NexiElencoServiziViewOracleRepository nexiElencoServiziViewOracleRepository;
    @Autowired(required = false)
    private NexiElencoServiziPostgresRepository nexiElencoServiziPostgresRepository;
    @Autowired(required = false)
    private NexiElencoServiziViewPostgresRepository nexiElencoServiziViewPostgresRepository;

    @Autowired(required = false)
    private PagoPaCdiPreferencesPostgresRepository pagoPaCdiPreferencesPostgresRepository;
    @Autowired(required = false)
    private PagoPaCdiPreferencesViewPostgresRepository pagoPaCdiPreferencesViewPostgresRepository;
    @Autowired(required = false)
    private NexiCdiPreferencesOracleRepository nexiCdiPreferencesOracleRepository;
    @Autowired(required = false)
    private NexiCdiPreferencesViewOracleRepository nexiCdiPreferencesViewOracleRepository;
    @Autowired(required = false)
    private NexiCdiPreferencesPostgresRepository nexiCdiPreferencesPostgresRepository;
    @Autowired(required = false)
    private NexiCdiPreferencesViewPostgresRepository nexiCdiPreferencesViewPostgresRepository;

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

            if(riversamentoEnabled) {
                riversamentoElencoServizi();
                riversamentoCdiPreferences();
            }

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

    private void riversamentoElencoServizi() {
        log.info("riversamentoElencoServizi");
        JpaRepository<ElencoServiziView,Long> sourceRepository = null;
        JpaRepository<ElencoServizi,Long> targetRepository = null;
        switch (riversamentoSource){
            case "pagopa-postgres":
                sourceRepository = pagoPaElencoServiziViewPostgresRepository;
                break;
            case "nexi-oracle":
                sourceRepository = nexiElencoServiziViewOracleRepository;
                break;
            case "nexi-postgres":
                sourceRepository = nexiElencoServiziViewPostgresRepository;
                break;
        }

        switch (riversamentoTarget){
            case "pagopa-postgres":
                targetRepository = pagoPaElencoServiziPostgresRepository;
                break;
            case "nexi-oracle":
                targetRepository = nexiElencoServiziOracleRepository;
                break;
            case "nexi-postgres":
                targetRepository = nexiElencoServiziPostgresRepository;
                break;
        }
        if(sourceRepository == null || targetRepository == null){
            log.error("riversamentoElencoServizi wrong riversamentoSource[{}] or riversamentoTarget[{}]",riversamentoSource,riversamentoTarget);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }

        log.info("riversamentoElencoServizi deleting all from target");
        targetRepository.deleteAll();

        log.info("riversamentoElencoServizi getting data from source");
        Page<ElencoServiziView> page = null;
        do{
            if(page==null){
                page = sourceRepository.findAll(Pageable.ofSize(100));
            }else{
                page = sourceRepository.findAll(page.nextPageable());
            }
            List<ElencoServiziView> sources = page.toList();
            List<ElencoServizi> targets = sources.stream().map(s -> {
                return modelMapper.map(s, ElencoServizi.class);
            }).toList();
            log.info("riversamentoElencoServizi saving page {}/{} in target",page.getNumber()+1,page.getTotalPages());
            targetRepository.saveAll(targets);
            log.info("riversamentoElencoServizi saved");
        } while(page.hasNext());
        log.info("riversamentoElencoServizi done");

    }
    private void riversamentoCdiPreferences() {
        log.info("riversamentoCdiPreferences");

        JpaRepository<CDIPreferencesView,Long> sourceRepository = null;
        JpaRepository<CDIPreferences,Long> targetRepository = null;
        switch (riversamentoSource){
            case "pagopa-postgres":
                sourceRepository = pagoPaCdiPreferencesViewPostgresRepository;
                break;
            case "nexi-oracle":
                sourceRepository = nexiCdiPreferencesViewOracleRepository;
                break;
            case "nexi-postgres":
                sourceRepository = nexiCdiPreferencesViewPostgresRepository;
                break;
        }

        switch (riversamentoTarget){
            case "pagopa-postgres":
                targetRepository = pagoPaCdiPreferencesPostgresRepository;
                break;
            case "nexi-oracle":
                targetRepository = nexiCdiPreferencesOracleRepository;
                break;
            case "nexi-postgres":
                targetRepository = nexiCdiPreferencesPostgresRepository;
                break;
        }
        if(sourceRepository == null || targetRepository == null){
            log.error("riversamentoCdiPreferences wrong riversamentoSource[{}] or riversamentoTarget[{}]",riversamentoSource,riversamentoTarget);
            throw new AppException(AppError.INTERNAL_SERVER_ERROR);
        }
        log.info("riversamentoCdiPreferences deleting all from target");
        targetRepository.deleteAll();

        log.info("riversamentoCdiPreferences getting data from source");
        Page<CDIPreferencesView> page = null;
        do{
            if(page==null){
                page = sourceRepository.findAll(Pageable.ofSize(100));
            }else{
                page = sourceRepository.findAll(page.nextPageable());
            }
            List<CDIPreferencesView> sources = page.toList();
            List<CDIPreferences> targets = sources.stream().map(s -> {
                return modelMapper.map(s, CDIPreferences.class);
            }).toList();
            log.info("riversamentoCdiPreferences saving page {}/{} in target",page.getNumber()+1,page.getTotalPages());
            targetRepository.saveAll(targets);
            log.info("riversamentoCdiPreferences saved");
        } while(page.hasNext());
        log.info("riversamentoCdiPreferences done");

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
