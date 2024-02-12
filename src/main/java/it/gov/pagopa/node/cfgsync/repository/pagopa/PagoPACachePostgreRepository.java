package it.gov.pagopa.node.cfgsync.repository.pagopa;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoPACachePostgreRepository extends JpaRepository<ConfigCache, String> { }