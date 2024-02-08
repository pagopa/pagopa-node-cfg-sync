package it.gov.pagopa.node.cfg_sync.repository.pagopa.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "cache")
public class CachePagoPA implements Serializable {

  @Id
  @Column(name="ID", columnDefinition = "VARCHAR", length = 20)
  private String id;

  private LocalDateTime time;

  private byte[] cache;

  @Column(name="VERSION", columnDefinition = "VARCHAR", length = 32)
  private String version;

}
