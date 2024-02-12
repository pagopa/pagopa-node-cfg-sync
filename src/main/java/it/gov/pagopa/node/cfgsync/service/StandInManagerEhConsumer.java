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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "stand-in-manager.consumer", name = "enabled")
public class StandInManagerEhConsumer {

    @Value("${stand-in-manager.rx-connection-string}")
    private String standInManagerRxConnectionString;
    @Value("${stand-in-manager.rx-name}")
    private String standInManagerRxName;
    @Value("${stand-in-manager.sa-connection-string}")
    private String standInManagerSaConnectionString;
    @Value("${stand-in-manager.sa-name}")
    private String standInManagerSaContainerName;
    @Value("${stand-in-manager.consumer-group}")
    private String standInManagerConsumerGroup;

    @Autowired
    private CacheServiceFactory cacheServiceFactory;

    @Bean
    BlobContainerAsyncClient blobContainerStandInAsyncClient() {
        return new BlobContainerClientBuilder().connectionString(standInManagerSaConnectionString)
                .containerName(standInManagerSaContainerName).buildAsyncClient();
    }

    @Bean
    EventProcessorClient eventProcessorStandInClient(BlobContainerAsyncClient blobContainerAsyncClient) {
        return new EventProcessorClientBuilder().connectionString(standInManagerRxConnectionString)
                .consumerGroup(standInManagerConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(StandInManagerEhConsumer::processEvent)
                .processError(StandInManagerEhConsumer::processError).buildEventProcessorClient();
    }

    public static void processEvent(EventContext eventContext) {
        log.info("Processing event {} from partition {} with sequence number {} with body: {}",
                TargetRefreshEnum.standin.label,
                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
                eventContext.getEventData().getBodyAsString());
        CacheServiceFactory.getService(TargetRefreshEnum.config).sync();
    }

    public static void processError(ErrorContext errorContext) {
        log.error("Error occurred in partition processor {} for partition {}, {}",
                TargetRefreshEnum.standin.label,
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

}
