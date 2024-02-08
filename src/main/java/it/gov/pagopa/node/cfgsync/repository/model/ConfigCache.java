package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cache")
public class ConfigCache implements Serializable {

    @Id
    @Column(name="ID", columnDefinition = "VARCHAR", length = 20)
    private String id;

    private LocalDateTime time;

    private byte[] cache;

    @Column(name="VERSION", columnDefinition = "VARCHAR", length = 32)
    private String version;

}
