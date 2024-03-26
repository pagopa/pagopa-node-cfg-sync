package it.gov.pagopa.node.cfgsync;

import it.gov.pagopa.node.cfgsync.util.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
class ApplicationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void contextLoads() {
    // check only if the context is loaded
    assertTrue(true);
  }

  @Test
  void testConstructorIsPrivate() throws NoSuchMethodException {
    Constructor<Utils> constructor = Utils.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    assertThrows(InvocationTargetException.class, constructor::newInstance);
  }

  @Test
  void homeSwagger() {
    ResponseEntity<String> response = restTemplate.exchange("/", HttpMethod.GET, null, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertFalse(response.getBody().isEmpty());
    assertThat(response.getBody().contains("Swagger UI"));
  }

  @Test
  void constantsTest() {
    assertEquals("NEXIORACLE", ConstantsHelper.NEXIORACLE_SI);
    assertEquals("NEXIPOSTGRES", ConstantsHelper.NEXIPOSTGRES_SI);
    assertEquals("PAGOPAPOSTGRES", ConstantsHelper.PAGOPAPOSTGRES_SI);
  }

}
