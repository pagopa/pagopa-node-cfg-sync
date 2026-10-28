package it.gov.pagopa.node.cfgsync.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppInsightTelemetryClientConfig {

    @Value("${azure.application-insight.connection-string}")
    private String connectionString;

    @Bean
    public TelemetryClient telemetryClient() {
        TelemetryConfiguration aDefault = TelemetryConfiguration.createDefault();
        aDefault.setConnectionString(connectionString);
        return new TelemetryClient(aDefault);
    }
}
