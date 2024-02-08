package it.gov.pagopa.node.cfgsync.repository.model.nexi;

import it.gov.pagopa.node.cfgsync.repository.model.Cache;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "cache")
public class CacheNexiOracle extends Cache {

}
