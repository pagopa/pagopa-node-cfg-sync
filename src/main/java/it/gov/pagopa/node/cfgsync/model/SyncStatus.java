package it.gov.pagopa.node.cfgsync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncStatus {

    @NotNull
    private String serviceIdentifier;
    @NotNull private SyncStatusEnum status;
}
