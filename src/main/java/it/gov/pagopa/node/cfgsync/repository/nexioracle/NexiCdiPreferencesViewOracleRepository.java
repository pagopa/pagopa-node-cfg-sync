package it.gov.pagopa.node.cfgsync.repository.nexioracle;

import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferencesView;
import it.gov.pagopa.node.cfgsync.util.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.source",havingValue = Constants.NEXI_ORACLE)
public interface NexiCdiPreferencesViewOracleRepository extends JpaRepository<CDIPreferencesView, Long> { }