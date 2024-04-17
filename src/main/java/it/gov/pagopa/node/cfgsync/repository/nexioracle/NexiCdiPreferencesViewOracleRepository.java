package it.gov.pagopa.node.cfgsync.repository.nexioracle;

import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferencesView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NexiCdiPreferencesViewOracleRepository extends JpaRepository<CDIPreferencesView, Long> { }