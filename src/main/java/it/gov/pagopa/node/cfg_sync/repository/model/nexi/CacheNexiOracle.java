package it.gov.pagopa.node.cfg_sync.repository.model.nexi;

import it.gov.pagopa.node.cfg_sync.repository.model.Cache;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cache")
public class CacheNexiOracle extends Cache {

}
