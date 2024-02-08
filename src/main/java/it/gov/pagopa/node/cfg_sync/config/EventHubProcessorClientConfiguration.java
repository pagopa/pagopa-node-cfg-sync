package it.gov.pagopa.node.cfg_sync.config;

import com.azure.spring.cloud.service.eventhubs.consumer.EventHubsErrorHandler;
import com.azure.spring.cloud.service.eventhubs.consumer.EventHubsRecordMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventHubProcessorClientConfiguration {

    @Bean
    EventHubsRecordMessageListener processEvent() {
        return eventContext->log.info("Processing event from partition {} with sequence number {} with body: {}",
            eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber(),
            eventContext.getEventData().getBodyAsString());
    }

    @Bean
    EventHubsErrorHandler processError() {
        return errorContext->log.info("Error occurred in partition processor for partition {}, {}",
            errorContext.getPartitionContext().getPartitionId(),
            errorContext.getThrowable().getMessage(),
            errorContext.getThrowable());
    }

}