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
@ConditionalOnProperty(prefix = "api-config-cache.consumer", name = "enabled")
public class ApiConfigCacheEhConsumer {

    @Value("${api-config-cache.rx-connection-string}")
    private String configCacheRxConnectionString;
    @Value("${api-config-cache.rx-name}")
    private String configCacheRxName;
    @Value("${api-config-cache.sa-connection-string}")
    private String configCacheSaConnectionString;
    @Value("${api-config-cache.sa-name}")
    private String configCacheSaContainerName;
    @Value("${api-config-cache.consumer-group}")
    private String configCacheConsumerGroup;

    private final ApiConfigCacheService apiConfigCacheService;

    @PostConstruct
    public void post(){
        BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder().connectionString(configCacheSaConnectionString)
                .containerName(configCacheSaContainerName).buildAsyncClient();
        EventProcessorClient eventProcessorClient = new EventProcessorClientBuilder().connectionString(configCacheRxConnectionString)
                .consumerGroup(configCacheConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(this::processEvent)
                .processError(this::processError).buildEventProcessorClient();
        eventProcessorClient.start();
    }

    public void processEvent(EventContext eventContext) {
        log.info("[{}] Processing event from partition {} with sequence number {} with body: {}",
                TargetRefreshEnum.cache.label,
                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
                eventContext.getEventData().getBodyAsString());

        try {
            apiConfigCacheService.syncCache();
        } catch (Exception ex) {
            log.error("[{}][ALERT] Generic Error on consumer: {}", TargetRefreshEnum.cache.label, ex.getMessage(), ex);
        }

        try {
            apiConfigCacheService.syncRiversamento();
        } catch (Exception ex) {
            log.error("[{}][ALERT] Generic Error on consumer: {}", TargetRefreshEnum.riversamento.label, ex.getMessage(), ex);
        }


    }

    public void processError(ErrorContext errorContext) {
        log.error("[{}][ALERT] Error occurred from partition {}: {}",
                TargetRefreshEnum.cache.label,
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

}
