package it.gov.pagopa.node.cfgsync.repository.pagopa;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoPACachePostgresRepository extends JpaRepository<ConfigCache, String> { }