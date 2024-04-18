package it.gov.pagopa.node.cfgsync.repository.pagopa;

import it.gov.pagopa.node.cfgsync.repository.model.ElencoServiziView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.source",havingValue = "pagopa-postgres")
public interface PagoPaElencoServiziViewPostgresRepository extends JpaRepository<ElencoServiziView, Long> { }