package it.gov.pagopa.node.cfgsync.repository.pagopa;

import it.gov.pagopa.node.cfgsync.repository.model.CDIPreferencesView;
import it.gov.pagopa.node.cfgsync.util.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.source",havingValue = Constants.PAGOPA_POSTGRES)
public interface PagoPaCdiPreferencesViewPostgresRepository extends JpaRepository<CDIPreferencesView, Long> { }