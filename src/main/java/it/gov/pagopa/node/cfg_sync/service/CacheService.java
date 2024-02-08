package it.gov.pagopa.node.cfg_sync.service;

import it.gov.pagopa.node.cfg_sync.model.TargetRefreshEnum;

public interface CacheService {

    TargetRefreshEnum getType();
    void syncCache();
}
