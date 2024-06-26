package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;


@NoArgsConstructor
@Entity
@Table(name = "{cdi_preferences_target}")
@Setter
@Getter
public class CDIPreferences {

    @Column(name="OBJ_ID")
    @Id
    private Long objid;
    @Column(name="SELLER")
    private String seller;
    @Column(name="BUYER")
    private String buyer;
    @Column(name="COSTO_CONVENZIONE")
    private BigDecimal costoconvenzione;
    @Column(name="ID_CDI_MASTER")
    private Long idcdimaster;
}
