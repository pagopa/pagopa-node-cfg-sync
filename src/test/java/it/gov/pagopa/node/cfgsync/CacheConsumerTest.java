package it.gov.pagopa.node.cfgsync;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.messaging.eventhubs.models.LastEnqueuedEventProperties;
import com.azure.messaging.eventhubs.models.PartitionContext;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheEhConsumer;
import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CacheConsumerTest {

    @Mock
    ApiConfigCacheService service;

    @Test
    void processEvent() {
        PartitionContext partitionContext = new PartitionContext("", "", "", "1");
        CheckpointStore checkpointStore = new BlobCheckpointStore(null);
        LastEnqueuedEventProperties lastEnqueuedEventProperties = new LastEnqueuedEventProperties(1L, 1L, Instant.now(), Instant.now());
        EventData eventData = new EventData();
        EventContext eventContext = new EventContext(partitionContext, eventData, checkpointStore, lastEnqueuedEventProperties);

        ApiConfigCacheEhConsumer consumer = new ApiConfigCacheEhConsumer(service);
        consumer.processEvent(eventContext);
        verify(service, times(1)).syncCache();
    }

    @Test
    void processError() {
        PartitionContext partitionContext = new PartitionContext("", "", "", "1");
        ErrorContext errorContext = new ErrorContext(partitionContext, new Exception(""));

        ApiConfigCacheEhConsumer consumer = new ApiConfigCacheEhConsumer(service);
        consumer.processError(errorContext);
        verify(service, times(0)).syncCache();
    }

}
