package it.gov.pagopa.node.cfg_sync.repository.model;

import antlr.ANTLRParser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.LocalDateTime;


@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class Cache implements Serializable {

    @Id
    @Column(name="ID", columnDefinition = "VARCHAR", length = 20)
    private String id;

    private LocalDateTime time;

    private byte[] cache;

    @Column(name="VERSION", columnDefinition = "VARCHAR", length = 32)
    private String version;

}
