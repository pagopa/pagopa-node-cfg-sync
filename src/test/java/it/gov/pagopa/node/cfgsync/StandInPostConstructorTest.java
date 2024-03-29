package it.gov.pagopa.node.cfgsync;

import it.gov.pagopa.node.cfgsync.service.StandInManagerService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
class StandInPostConstructorTest {

    @Test
    void postConstruct() {
        StandInManagerService postConstructChild = Mockito.mock(StandInManagerService.class);
        InOrder inOrder = Mockito.inOrder(postConstructChild);
        inOrder.verify(postConstructChild, Mockito.times(1));
    }

}
