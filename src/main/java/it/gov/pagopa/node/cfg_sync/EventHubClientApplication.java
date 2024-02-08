package it.gov.pagopa.node.cfg_sync;

import com.azure.messaging.eventhubs.EventProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class EventHubClientApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHubClientApplication.class);
//    private final EventHubProducerClient eventHubProducerClient;
    private final EventProcessorClient eventProcessorClient;

    public EventHubClientApplication(//EventHubProducerClient eventHubProducerClient,
                                     EventProcessorClient eventProcessorClient) {
//        this.eventHubProducerClient = eventHubProducerClient;
        this.eventProcessorClient = eventProcessorClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(EventHubClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        eventProcessorClient.start();
        // Wait for the processor client to be ready
        TimeUnit.SECONDS.sleep(10);

//        eventHubProducerClient.send(Collections.singletonList(new EventData("Hello World")));
//        LOGGER.info("Successfully sent a message to Event Hubs.");
//        eventHubProducerClient.close();
        LOGGER.info("Stopping and closing the processor");
        eventProcessorClient.stop();
    }

}