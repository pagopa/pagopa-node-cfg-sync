package it.gov.pagopa.node.cfg_sync.repository.nexi;

import it.gov.pagopa.node.cfg_sync.repository.model.nexi.CacheNexiOracle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheNodoONexiRepository extends JpaRepository<CacheNexiOracle, String> { }