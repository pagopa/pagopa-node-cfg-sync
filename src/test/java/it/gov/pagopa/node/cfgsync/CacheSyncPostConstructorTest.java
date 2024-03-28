package it.gov.pagopa.node.cfgsync;

import it.gov.pagopa.node.cfgsync.service.ApiConfigCacheService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
class CacheSyncPostConstructorTest {

    @Test
    void postConstruct() {
//        final ApiConfigCacheService postConstructChild = Mockito.mock(ApiConfigCacheService.class);
//        InOrder inOrder = Mockito.inOrder(postConstructChild);
//        inOrder.verify(postConstructChild, Mockito.times(1));
        ApiConfigCacheService mockInstance = Mockito.mock(ApiConfigCacheService.class);
        Mockito.verify(mockInstance, Mockito.times(1)).setApiConfigCacheClient(null);
    }

}
