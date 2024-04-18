package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "{cdi_preferences_target}")
@Setter
@Getter
public class CDIPreferences {

    @Column(name="OBJ_ID")
    @Id
    private Long OBJ_ID;
    @Column(name="SELLER")
    private String SELLER;
    @Column(name="BUYER")
    private String BUYER;
    @Column(name="COSTO_CONVENZIONE")
    private BigDecimal COSTO_CONVENZIONE;
    @Column(name="ID_CDI_MASTER")
    private Long ID_CDI_MASTER;
}
