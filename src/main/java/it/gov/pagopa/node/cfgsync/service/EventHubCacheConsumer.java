package it.gov.pagopa.node.cfgsync.service;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventHubCacheConsumer {

    @Value("${nodo-dei-pagamenti-cache-rx-connection-string}")
    private String nodoCacheRxConnectionString;
    @Value("${nodo-dei-pagamenti-cache-rx-name}")
    private String nodoCacheRxName;
    @Value("${nodo-dei-pagamenti-cache-sa-connection-string}")
    private String nodoCacheSaConnectionString;
    @Value("${nodo-dei-pagamenti-cache-sa-name}")
    private String nodoCacheSaContainerName;
    @Value("${nodo-dei-pagamenti-cache-consumer-group}")
    private String nodoCacheConsumerGroup;

    @Autowired
    private CacheServiceFactory cacheServiceFactory;

//    @Bean
//    EventHubClientBuilder eventHubClientBuilder() {
//        return new EventHubClientBuilder().connectionString(nodoCacheRxConnectionString);
//    }

//    @Bean
//    BlobContainerClientBuilder blobContainerClientBuilder() {
//        return new BlobContainerClientBuilder().connectionString(nodoCacheSaConnectionString)
//                .containerName(nodoCacheSaContainerName);
//    }

    @Bean
    BlobContainerAsyncClient blobContainerAsyncClient() {
        return new BlobContainerClientBuilder().connectionString(nodoCacheSaConnectionString)
                .containerName(nodoCacheSaContainerName).buildAsyncClient();
    }

//    @Bean
//    EventProcessorClientBuilder eventProcessorClientBuilder(BlobContainerAsyncClient blobContainerAsyncClient) {
//        return new EventProcessorClientBuilder().connectionString(nodoCacheRxConnectionString)
//                .consumerGroup(nodoCacheConsumerGroup)
//                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
//                .processEvent(NodoCacheEHClientConfiguration::processEvent)
//                .processError(NodoCacheEHClientConfiguration::processError);
//    }

    @Bean
    EventProcessorClient eventProcessorClient(BlobContainerAsyncClient blobContainerAsyncClient) {
        return new EventProcessorClientBuilder().connectionString(nodoCacheRxConnectionString)
                .consumerGroup(nodoCacheConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(EventHubCacheConsumer::processEvent)
                .processError(EventHubCacheConsumer::processError).buildEventProcessorClient();
    }

    public static void processEvent(EventContext eventContext) {
        log.info("Processing event from partition {} with sequence number {} with body: {}",
                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
                eventContext.getEventData().getBodyAsString());
        CacheServiceFactory.getService(TargetRefreshEnum.config).sync();
    }

    public static void processError(ErrorContext errorContext) {
        log.error("Error occurred in partition processor for partition {}, {}",
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

}
