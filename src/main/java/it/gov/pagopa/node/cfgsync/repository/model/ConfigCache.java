package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cache")
public class ConfigCache {

    @Id
    @Column(name="ID", nullable = false, columnDefinition = "VARCHAR", length = 20)
    private String id;

    @Column(name = "TIME", nullable = false)
    private ZonedDateTime time;

    @Column(name = "CACHE", nullable = false)
    private byte[] cache;

    @Column(name="VERSION", nullable = false, columnDefinition = "VARCHAR", length = 32)
    private String version;

}
