package it.gov.pagopa.node.cfgsync.repository.nexipostgre.cache;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NexiCachePostgreRepository extends JpaRepository<ConfigCache, String> { }