package it.gov.pagopa.node.cfgsync;

import it.gov.pagopa.node.cfgsync.client.AppInsightTelemetryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@Configuration
public class TestConfig {
    @Bean
    @Primary
    public AppInsightTelemetryClient appInsightTelemetryClient() {
        // This provides a mock bean instead of the real one.
        return Mockito.mock(AppInsightTelemetryClient.class);
    }
}