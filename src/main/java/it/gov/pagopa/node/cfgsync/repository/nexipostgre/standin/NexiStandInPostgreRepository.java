package it.gov.pagopa.node.cfgsync.repository.nexipostgre.standin;

import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NexiStandInPostgreRepository extends JpaRepository<StandInStations, String> { }