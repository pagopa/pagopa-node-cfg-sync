package it.gov.pagopa.node.cfgsync.repository.nexipostgres;

import it.gov.pagopa.node.cfgsync.repository.model.ElencoServizi;
import it.gov.pagopa.node.cfgsync.util.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "riversamento.target",havingValue = Constants.NEXI_POSTGRES)
public interface NexiElencoServiziPostgresRepository extends JpaRepository<ElencoServizi, Long> { }