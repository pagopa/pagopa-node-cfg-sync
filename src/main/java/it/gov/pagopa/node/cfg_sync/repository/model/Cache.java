package it.gov.pagopa.node.cfg_sync.repository.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table
public class Cache implements Serializable {

  private String id;

  private LocalDateTime time;

  private byte[] cache;

  private String version;

}
