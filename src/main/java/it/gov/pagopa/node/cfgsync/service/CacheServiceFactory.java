package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CacheServiceFactory {

    @Autowired
    private List<CacheService> services;

    private static final Map<TargetRefreshEnum, CacheService> myServiceCache = new HashMap<>();

    @PostConstruct
    public void initMyServiceCache() {
        for(CacheService service : services) {
            myServiceCache.put(service.getType(), service);
        }
    }

    public static CacheService getService(TargetRefreshEnum target) {
        CacheService service = myServiceCache.get(target);
        if(service == null) throw new RuntimeException("Unknown service: " + target.label);
        return service;
    }
}
