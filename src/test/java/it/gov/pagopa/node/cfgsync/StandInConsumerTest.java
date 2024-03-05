//package it.gov.pagopa.node.cfgsync;
//
//import com.azure.messaging.eventhubs.models.EventContext;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import feign.Feign;
//import feign.mock.MockClient;
//import feign.mock.MockTarget;
//import it.gov.pagopa.node.cfgsync.client.StandInManagerClient;
//import it.gov.pagopa.node.cfgsync.model.ProblemJson;
//import it.gov.pagopa.node.cfgsync.model.StationsResponse;
//import it.gov.pagopa.node.cfgsync.model.SyncStatusEnum;
//import it.gov.pagopa.node.cfgsync.model.SyncStatusResponse;
//import it.gov.pagopa.node.cfgsync.repository.pagopa.PagoPAStandInPostgresRepository;
//import it.gov.pagopa.node.cfgsync.service.StandInManagerEhConsumer;
//import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentMatchers;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.List;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.hamcrest.Matchers.any;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.mock;
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
//
//
//    StandInManagerEhConsumer consumerClient = mock(StandInManagerEhConsumer.class);
//
//    when(consumerClient.processEvent(any())).thenReturn(eventsPublisher.flux());
//
//    standInManagerEhConsumer.processEvent(eventContext);
//
//    Mockito.when(standInManagerEhConsumer.processEvent(ArgumentMatchers.any()))
//            .thenAnswer(a -> Mono.just(new Object()));
//
//    given(standInManagerEhConsumer.processEvent(eventContext)).then( v -> "");
//    assertThat("pippo").isEqualTo("pippo");
//  }
//
//}
