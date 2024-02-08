//package it.gov.pagopa.node.cfg_sync.config;
//
//import com.azure.spring.integration.eventhubs.inbound.EventHubsInboundChannelAdapter;
//import com.azure.spring.messaging.eventhubs.core.EventHubsProcessorFactory;
//import com.azure.spring.messaging.eventhubs.core.checkpoint.CheckpointConfig;
//import com.azure.spring.messaging.eventhubs.core.checkpoint.CheckpointMode;
//import com.azure.spring.messaging.eventhubs.core.listener.EventHubsMessageListenerContainer;
//import com.azure.spring.messaging.eventhubs.core.properties.EventHubsContainerProperties;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.integration.annotation.ServiceActivator;
//import org.springframework.integration.channel.DirectChannel;
//import org.springframework.messaging.MessageChannel;
//
//@Configuration
//@Slf4j
//public class MessageReceiveConfiguration {
//
//    private static final String INPUT_CHANNEL = "input";
//    private static final String EVENT_HUB_NAME = "<your-event-hub-name>";
//    private static final String CONSUMER_GROUP = "$Default";
//
//    @ServiceActivator(inputChannel = INPUT_CHANNEL)
//    public void messageReceiver(byte[] payload) {
//        String message = new String(payload);
//        log.info("New message received: {}", message);
//    }
//
//    @Bean
//    public EventHubsMessageListenerContainer messageListenerContainer(EventHubsProcessorFactory processorFactory) {
//        EventHubsContainerProperties containerProperties = new EventHubsContainerProperties();
//        containerProperties.setEventHubName(EVENT_HUB_NAME);
//        containerProperties.setConsumerGroup(CONSUMER_GROUP);
//        containerProperties.setCheckpointConfig(new CheckpointConfig(CheckpointMode.MANUAL));
//        return new EventHubsMessageListenerContainer(processorFactory, containerProperties);
//    }
//
//    @Bean
//    public EventHubsInboundChannelAdapter messageChannelAdapter(@Qualifier(INPUT_CHANNEL) MessageChannel inputChannel,
//                                                                EventHubsMessageListenerContainer listenerContainer) {
//        EventHubsInboundChannelAdapter adapter = new EventHubsInboundChannelAdapter(listenerContainer);
//        adapter.setOutputChannel(inputChannel);
//        return adapter;
//    }
//
//    @Bean
//    public MessageChannel input() {
//        return new DirectChannel();
//    }
//
//}