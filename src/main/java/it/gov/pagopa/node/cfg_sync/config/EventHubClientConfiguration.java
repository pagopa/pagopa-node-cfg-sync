//package it.gov.pagopa.node.cfg_sync.config;
//
//import com.azure.identity.DefaultAzureCredentialBuilder;
//import com.azure.messaging.eventhubs.EventHubClientBuilder;
//import com.azure.messaging.eventhubs.EventHubProducerClient;
//import com.azure.messaging.eventhubs.EventProcessorClient;
//import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
//import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
//import com.azure.messaging.eventhubs.models.ErrorContext;
//import com.azure.messaging.eventhubs.models.EventContext;
//import com.azure.storage.blob.BlobContainerAsyncClient;
//import com.azure.storage.blob.BlobContainerClientBuilder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class EventHubClientConfiguration {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(EventHubClientConfiguration.class);
//    private static final String CONSUMER_GROUP = "$Default";
//    private static final String STORAGE_ACCOUNT_ENDPOINT = "https://pagopadapiconfigfesa.blob.core.windows.net/";
//    private static final String STORAGE_CONTAINER_NAME = "cfg-sync";
//
//    @Value("${nodo-dei-pagamenti-cache-rx-connection-string}")
//    private String ndpConnectionString;
//
//    @Value("${nodo-dei-pagamenti-cache-rx-name}")
//    private String ndpEventHubName;
//
//    @Bean
//    EventHubClientBuilder eventHubClientBuilder() {
//        return new EventHubClientBuilder().credential(ndpConnectionString, ndpEventHubName,
//                new DefaultAzureCredentialBuilder()
//                        .build());
//    }
//
//    @Bean
//    BlobContainerClientBuilder blobContainerClientBuilder() {
//        return new BlobContainerClientBuilder().credential(new DefaultAzureCredentialBuilder()
//                        .build())
//                .endpoint(STORAGE_ACCOUNT_ENDPOINT)
//                .containerName(STORAGE_CONTAINER_NAME);
//    }
//
//    @Bean
//    BlobContainerAsyncClient blobContainerAsyncClient(BlobContainerClientBuilder blobContainerClientBuilder) {
//        return blobContainerClientBuilder.buildAsyncClient();
//    }
//
//    @Bean
//    EventProcessorClientBuilder eventProcessorClientBuilder(BlobContainerAsyncClient blobContainerAsyncClient) {
//        return new EventProcessorClientBuilder().credential(ndpConnectionString, ndpEventHubName,
//                        new DefaultAzureCredentialBuilder()
//                                .build())
//                .consumerGroup(CONSUMER_GROUP)
//                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
//                .processEvent(EventHubClientConfiguration::processEvent)
//                .processError(EventHubClientConfiguration::processError);
//    }
//
//    @Bean
//    EventProcessorClient eventProcessorClient(EventProcessorClientBuilder eventProcessorClientBuilder) {
//        return eventProcessorClientBuilder.buildEventProcessorClient();
//    }
//
//    public static void processEvent(EventContext eventContext) {
//        LOGGER.info("Processing event from partition {} with sequence number {} with body: {}",
//                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
//                eventContext.getEventData().getBodyAsString());
//    }
//
//    public static void processError(ErrorContext errorContext) {
//        LOGGER.info("Error occurred in partition processor for partition {}, {}",
//                errorContext.getPartitionContext().getPartitionId(),
//                errorContext.getThrowable().getMessage(),
//                errorContext.getThrowable());
//    }
//
//}
