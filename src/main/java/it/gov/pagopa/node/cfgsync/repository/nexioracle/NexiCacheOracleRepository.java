package it.gov.pagopa.node.cfgsync.repository.nexioracle;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NexiCacheOracleRepository extends JpaRepository<ConfigCache, String> { }