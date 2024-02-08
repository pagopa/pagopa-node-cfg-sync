package it.gov.pagopa.node.cfgsync;

import com.azure.messaging.eventhubs.EventProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class EventHubClientApplication implements CommandLineRunner {

    private final EventProcessorClient eventProcessorClient;

    public EventHubClientApplication(EventProcessorClient eventProcessorClient) {
        this.eventProcessorClient = eventProcessorClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(EventHubClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        eventProcessorClient.start();
    }

}