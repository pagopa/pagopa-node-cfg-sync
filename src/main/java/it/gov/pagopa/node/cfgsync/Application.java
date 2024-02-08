//package it.gov.pagopa.node.cfg_sync;
//
//import com.azure.messaging.eventhubs.EventData;
//import com.azure.messaging.eventhubs.EventHubProducerClient;
//import com.azure.messaging.eventhubs.EventProcessorClient;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
//
//import java.util.Collections;
//import java.util.concurrent.TimeUnit;
//
//@SpringBootApplication
//@EnableAutoConfiguration(exclude = {
//        DataSourceAutoConfiguration.class,
//        DataSourceTransactionManagerAutoConfiguration.class,
//        HibernateJpaAutoConfiguration.class})
//@Slf4j
//public class Application implements CommandLineRunner {
//
////  private final EventHubProducerClient eventHubProducerClient;
//  private final EventProcessorClient eventProcessorClient;
//
//  public Application(//EventHubProducerClient eventHubProducerClient,
//                                   EventProcessorClient eventProcessorClient) {
////    this.eventHubProducerClient = eventHubProducerClient;
//    this.eventProcessorClient = eventProcessorClient;
//  }
//
//  public static void main(String[] args) {
//    SpringApplication.run(Application.class, args);
//  }
//
//  @Override
//  public void run(String... args) throws Exception {
//    eventProcessorClient.start();
//    // Wait for the processor client to be ready
//    TimeUnit.SECONDS.sleep(10);
//
////    eventHubProducerClient.send(Collections.singletonList(new EventData("Hello World")));
////    log.info("Successfully sent a message to Event Hubs.");
////    eventHubProducerClient.close();
//    log.info("Stopping and closing the processor");
//    eventProcessorClient.stop();
//  }
//
//}
