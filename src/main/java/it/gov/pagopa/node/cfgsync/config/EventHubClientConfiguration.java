package it.gov.pagopa.node.cfgsync.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventHubClientConfiguration {

    private static final String CONSUMER_GROUP = "$Default";

    @Value("${nodo-dei-pagamenti-cache-rx-connection-string}")
    private String nodoCacheRxConnectionString;
    @Value("${nodo-dei-pagamenti-cache-rx-name}")
    private String nodoCacheRxName;
    @Value("${nodo-dei-pagamenti-cache-sa-connection-string}")
    private String nodoCacheSaConnectionString;
    @Value("${nodo-dei-pagamenti-cache-sa-name}")
    private String nodoCacheSaContainerName;

    @Bean
    EventHubClientBuilder eventHubClientBuilder() {
        return new EventHubClientBuilder().connectionString(nodoCacheRxConnectionString);
    }

    @Bean
    BlobContainerClientBuilder blobContainerClientBuilder() {
        return new BlobContainerClientBuilder().connectionString(nodoCacheSaConnectionString)
                .containerName(nodoCacheSaContainerName);
    }

    @Bean
    BlobContainerAsyncClient blobContainerAsyncClient(BlobContainerClientBuilder blobContainerClientBuilder) {
        return blobContainerClientBuilder.buildAsyncClient();
    }

    @Bean
    EventProcessorClientBuilder eventProcessorClientBuilder(BlobContainerAsyncClient blobContainerAsyncClient) {
        return new EventProcessorClientBuilder().connectionString(nodoCacheRxConnectionString)
                .consumerGroup(CONSUMER_GROUP)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(EventHubClientConfiguration::processEvent)
                .processError(EventHubClientConfiguration::processError);
    }

    @Bean
    EventProcessorClient eventProcessorClient(EventProcessorClientBuilder eventProcessorClientBuilder) {
        return eventProcessorClientBuilder.buildEventProcessorClient();
    }

    public static void processEvent(EventContext eventContext) {
        log.info("Processing event from partition {} with sequence number {} with body: {}",
                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
                eventContext.getEventData().getBodyAsString());
    }

    public static void processError(ErrorContext errorContext) {
        log.info("Error occurred in partition processor for partition {}, {}",
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

}
