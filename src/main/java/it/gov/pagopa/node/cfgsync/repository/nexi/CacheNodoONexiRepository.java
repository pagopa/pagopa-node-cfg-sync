package it.gov.pagopa.node.cfgsync.repository.nexi;

import it.gov.pagopa.node.cfgsync.repository.model.nexi.CacheNexiOracle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheNodoONexiRepository extends JpaRepository<CacheNexiOracle, String> { }