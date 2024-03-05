package it.gov.pagopa.node.cfgsync.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandInManagerResponse {

    private List<String> stations;
}
