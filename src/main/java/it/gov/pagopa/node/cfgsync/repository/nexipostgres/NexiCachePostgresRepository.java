package it.gov.pagopa.node.cfgsync.repository.nexipostgres;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "api-config-cache.write.nexi-postgres")
public interface NexiCachePostgresRepository extends JpaRepository<ConfigCache, String> { }