package it.gov.pagopa.node.cfgsync.repository.nexioracle;

import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferences;
import it.gov.pagopa.node.cfgsync.repository.model.ElencoServizi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.target",havingValue = "nexi-oracle")
public interface NexiCdiPreferencesOracleRepository extends JpaRepository<CDIPreferences, Long> { }