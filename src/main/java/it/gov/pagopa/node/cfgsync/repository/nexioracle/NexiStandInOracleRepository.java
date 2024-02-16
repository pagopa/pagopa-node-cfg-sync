package it.gov.pagopa.node.cfgsync.repository.nexioracle;

import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NexiStandInOracleRepository extends JpaRepository<StandInStations, String> { }