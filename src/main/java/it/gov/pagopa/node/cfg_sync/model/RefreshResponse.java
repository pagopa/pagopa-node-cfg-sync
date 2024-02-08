package it.gov.pagopa.node.cfg_sync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefreshResponse {

  @NotNull private String id;
//  @NotNull private String version;
  @NotNull private ZonedDateTime timestamp;
}
