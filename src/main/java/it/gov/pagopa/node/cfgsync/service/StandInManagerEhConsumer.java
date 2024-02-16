package it.gov.pagopa.node.cfgsync.service;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import it.gov.pagopa.node.cfgsync.model.TargetRefreshEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
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

    private final StandInManagerService standInManagerService;

    @PostConstruct
    public void post(){
        BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder().connectionString(standInManagerSaConnectionString)
                .containerName(standInManagerSaContainerName).buildAsyncClient();
        EventProcessorClient eventProcessorClient = new EventProcessorClientBuilder().connectionString(standInManagerRxConnectionString)
                .consumerGroup(standInManagerConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(this::processEvent)
                .processError(this::processError).buildEventProcessorClient();
        eventProcessorClient.start();
    }

    public void processEvent(EventContext eventContext) {
        log.info("Processing event {} from partition {} with sequence number {} with body: {}",
                TargetRefreshEnum.standin.label,
                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
                eventContext.getEventData().getBodyAsString());
        standInManagerService.forceStandIn();
    }

    public void processError(ErrorContext errorContext) {
        log.error("Error occurred in partition processor {} for partition {}, {}",
                TargetRefreshEnum.standin.label,
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

}
