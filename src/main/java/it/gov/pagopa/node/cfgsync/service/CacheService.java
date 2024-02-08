package it.gov.pagopa.node.cfgsync.service;

import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;

public interface CacheService {

    TargetRefreshEnum getType();
    void sync();
}
