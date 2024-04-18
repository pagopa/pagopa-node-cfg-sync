package it.gov.pagopa.node.cfgsync.repository.pagopa;

import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferences;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.target",havingValue = "pagopa-postgres")
public interface PagoPaCdiPreferencesPostgresRepository extends JpaRepository<CDIPreferences, Long> { }