package it.gov.pagopa.node.cfg_sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@Slf4j
public class Application implements CommandLineRunner {

//  private final EventProcessorClient eventProcessorClient;
//
//  public Application(EventProcessorClient eventProcessorClient) {
//    this.eventProcessorClient = eventProcessorClient;
//  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
//    eventProcessorClient.start();
//    // Wait for the processor client to be ready
//    TimeUnit.SECONDS.sleep(10);
//
//    log.info("Stopping and closing the processor");
//    eventProcessorClient.stop();
  }

}
