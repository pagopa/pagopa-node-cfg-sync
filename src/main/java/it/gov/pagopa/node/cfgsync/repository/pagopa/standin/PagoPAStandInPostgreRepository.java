package it.gov.pagopa.node.cfgsync.repository.pagopa.standin;

import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoPAStandInPostgreRepository extends JpaRepository<StandInStations, String> { }