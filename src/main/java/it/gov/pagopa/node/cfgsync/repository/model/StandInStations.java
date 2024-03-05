package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stand_in_stations")
public class StandInStations {

    @Id
    @Column(name="STATION_CODE", columnDefinition = "VARCHAR", length = 35)
    private String stationCode;

}
