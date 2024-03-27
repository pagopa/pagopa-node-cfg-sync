package it.gov.pagopa.node.cfgsync;

import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<Utils> constructor = Utils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void utilsTrimException() {
        assertThrows(SyncDbStatusException.class,
                () -> Utils.trimValueColumn(ConfigCache.class, "UNKNOWN_FIELD", StringUtils.repeat("*", 50)));
    }
}
