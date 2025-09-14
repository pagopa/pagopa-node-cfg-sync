package it.gov.pagopa.node.cfgsync.client;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import it.gov.pagopa.node.cfgsync.exception.AppError;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Azure Application Insight Telemetry client */
@Service
public class AppInsightTelemetryClient {

  private final String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");

  private final TelemetryClient telemetryClient;

  public AppInsightTelemetryClient() {
    TelemetryConfiguration aDefault = TelemetryConfiguration.createDefault();
    aDefault.setConnectionString(connectionString);
    this.telemetryClient = new TelemetryClient(aDefault);
  }

  /**
   * Create a custom event on Application Insight with the provided information
   *
   * @param errorCode the application error code
   * @param details details of the custom event
   * @param e exception added to the custom event
   */
  public void createCustomEventForAlert(AppError errorCode, String details, Exception e) {
    String errorMessage = null;
      if (e != null) {
          errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
      }
      Map<String, String> props =
        Map.of(
            "type",
            errorCode.getTitle(),
            "title",
            errorCode.getDetails(),
            "details",
            details,
            "cause",
            e != null ? errorMessage: "N/A");
    this.telemetryClient.trackEvent("NODE_CFG_SYNC", props, null);
  }
}
