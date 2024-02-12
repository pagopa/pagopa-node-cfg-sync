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

    @Autowired
    private CacheServiceFactory cacheServiceFactory;

    @Bean
    BlobContainerAsyncClient blobContainerAsyncClient() {
        return new BlobContainerClientBuilder().connectionString(configCacheSaConnectionString)
                .containerName(configCacheSaContainerName).buildAsyncClient();
    }

    @Bean
    EventProcessorClient eventProcessorClient(BlobContainerAsyncClient blobContainerAsyncClient) {
        return new EventProcessorClientBuilder().connectionString(configCacheRxConnectionString)
                .consumerGroup(configCacheConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                .processEvent(ApiConfigCacheEhConsumer::processEvent)
                .processError(ApiConfigCacheEhConsumer::processError).buildEventProcessorClient();
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
