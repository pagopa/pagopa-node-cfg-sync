//package it.gov.pagopa.node.cfgsync;
//
//import com.azure.messaging.eventhubs.models.EventContext;
//import feign.mock.MockClient;
//import it.gov.pagopa.node.cfgsync.service.StandInManagerEhConsumer;
//import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentMatchers;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//@RunWith(SpringRunner.class)
//class StandInConsumerTest {
//
//  public static final String STATIONS_PATH = "/stations";
//
//  @Autowired private StandInManagerService standInManagerService;
//  @Autowired private TestRestTemplate restTemplate;
//  private MockClient mockClient;
//
//  @Mock StandInManagerEhConsumer standInManagerEhConsumer;
//
//  @InjectMocks
//  private EventContext eventContext;
//
//  @Test
//  void syncStandIn_400() {
//    when(standInManagerEhConsumer.processEvent(ArgumentMatchers.any()))
//            .thenAnswer(a -> Mono.just(new Object()));
//
//    given(standInManagerEhConsumer.processEvent(eventContext)).then( v -> "");
//    assertThat("pippo").isEqualTo("pippo");
//  }
//
//}
