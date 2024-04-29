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
    private Long objid;
    @Column(name="PSP_ID")
    private String pspid;
    @Column(name="PSP_RAG_SOC")
    private String pspragsoc;
    @Column(name="PSP_FLAG_STORNO")
    private String pspflagstorno;
    @Column(name="PSP_FLAG_BOLLO")
    private String pspflagbollo;
    @Column(name="LOGO_PSP")
    private byte[] logopsp;
    @Column(name="FLUSSO_ID")
    private String flussoid;
    @Column(name="INTM_ID")
    private String intmid;
    @Column(name="CANALE_ID")
    private String canaleid;
    @Column(name="NOME_SERVIZIO")
    private String nomeservizio;
    @Column(name="CANALE_MOD_PAG")
    private Long canalemodpag;
    @Column(name="TIPO_VERS_COD")
    private String tipoverscod;
    @Column(name="CODICE_LINGUA")
    private String codicelingua;
    @Column(name="INF_COND_EC_MAX")
    private String infcondecmax;
    @Column(name="INF_DESC_SERV")
    private String infdescserv;
    @Column(name="INF_DISP_SERV")
    private String infdispserv;
    @Column(name="INF_URL_CANALE")
    private String infurlcanale;
    @Column(name="TIMESTAMP_INS")
    private LocalDate timestampins;
    @Column(name="DATA_VALIDITA")
    private LocalDateTime datavalidita;
    @Column(name="IMPORTO_MINIMO")
    private BigDecimal importominimo;
    @Column(name="IMPORTO_MASSIMO")
    private BigDecimal importomassimo;
    @Column(name="COSTO_FISSO")
    private BigDecimal costofisso;
    @Column(name="TAGS")
    private String tags;
    @Column(name="LOGO_SERVIZIO")
    private byte[] logoservizio;
    @Column(name="CANALE_APP")
    private String canaleapp;
    @Column(name="ON_US")
    private String onus;
    @Column(name="CARRELLO_CARTE")
    private String carrellocarte;
    @Column(name="CODICE_ABI")
    private String codiceabi;
    @Column(name="CODICE_MYBANK")
    private String codicemybank;
    @Column(name="CODICE_CONVENZIONE")
    private String codiceconvenzione;
    @Column(name="FLAG_IO")
    private String flagio;

}
