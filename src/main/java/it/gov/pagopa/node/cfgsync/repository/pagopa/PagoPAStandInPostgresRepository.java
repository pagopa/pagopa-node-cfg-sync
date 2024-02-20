package it.gov.pagopa.node.cfgsync.repository.pagopa;

import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoPAStandInPostgresRepository extends JpaRepository<StandInStations, String> { }