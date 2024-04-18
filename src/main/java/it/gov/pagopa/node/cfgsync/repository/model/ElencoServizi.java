package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "{elenco_servizi_target}")
@Setter
@Getter
public class ElencoServizi {

    @Column(name="OBJ_ID")
    @Id
    private Long OBJ_ID;
    @Column(name="PSP_ID")
    private String PSP_ID;
    @Column(name="PSP_RAG_SOC")
    private String PSP_RAG_SOC;
    @Column(name="PSP_FLAG_STORNO")
    private String PSP_FLAG_STORNO;
    @Column(name="PSP_FLAG_BOLLO")
    private String PSP_FLAG_BOLLO;
    @Column(name="LOGO_PSP")
    private byte[] LOGO_PSP;
    @Column(name="FLUSSO_ID")
    private String FLUSSO_ID;
    @Column(name="INTM_ID")
    private String INTM_ID;
    @Column(name="CANALE_ID")
    private String CANALE_ID;
    @Column(name="NOME_SERVIZIO")
    private String NOME_SERVIZIO;
    @Column(name="CANALE_MOD_PAG")
    private Long CANALE_MOD_PAG;
    @Column(name="TIPO_VERS_COD")
    private String TIPO_VERS_COD;
    @Column(name="CODICE_LINGUA")
    private String CODICE_LINGUA;
    @Column(name="INF_COND_EC_MAX")
    private String INF_COND_EC_MAX;
    @Column(name="INF_DESC_SERV")
    private String INF_DESC_SERV;
    @Column(name="INF_DISP_SERV")
    private String INF_DISP_SERV;
    @Column(name="INF_URL_CANALE")
    private String INF_URL_CANALE;
    @Column(name="TIMESTAMP_INS")
    private LocalDate TIMESTAMP_INS;
    @Column(name="DATA_VALIDITA")
    private LocalDateTime DATA_VALIDITA;
    @Column(name="IMPORTO_MINIMO")
    private BigDecimal IMPORTO_MINIMO;
    @Column(name="IMPORTO_MASSIMO")
    private BigDecimal IMPORTO_MASSIMO;
    @Column(name="COSTO_FISSO")
    private BigDecimal COSTO_FISSO;
    @Column(name="TAGS")
    private String TAGS;
    @Column(name="LOGO_SERVIZIO")
    private byte[] LOGO_SERVIZIO;
    @Column(name="CANALE_APP")
    private String CANALE_APP;
    @Column(name="ON_US")
    private String ON_US;
    @Column(name="CARRELLO_CARTE")
    private String CARRELLO_CARTE;
    @Column(name="CODICE_ABI")
    private String CODICE_ABI;
    @Column(name="CODICE_MYBANK")
    private String CODICE_MYBANK;
    @Column(name="CODICE_CONVENZIONE")
    private String CODICE_CONVENZIONE;
    @Column(name="FLAG_IO")
    private String FLAG_IO;

}
