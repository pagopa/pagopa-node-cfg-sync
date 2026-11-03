package it.gov.pagopa.node.cfgsync.repository.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stand_in_stations")
public class StandInStations {

    @Id
    @Column(name="STATION_CODE", columnDefinition = "VARCHAR", length = 35)
    private String stationCode;

}
