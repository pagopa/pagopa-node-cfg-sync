package it.gov.pagopa.node.cfgsync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncStatusResponse {

  @NotNull
  @Schema(
          example = "NDP001",
          description = "Database service identifier")
  private String serviceIdentifier;
  @NotNull
  @Schema(
          example = "done",
          description = "Database sync status result")
  private SyncStatusEnum status;
}
