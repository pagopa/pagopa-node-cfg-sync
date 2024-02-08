package it.gov.pagopa.node.cfgsync.repository.nexipostgre;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheNodoNexiORepository extends JpaRepository<ConfigCache, String> { }