package it.gov.pagopa.node.cfgsync.repository.nexipostgres;

import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferencesView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.source",havingValue = "nexi-postgres")
public interface NexiCdiPreferencesViewPostgresRepository extends JpaRepository<CDIPreferencesView, Long> { }