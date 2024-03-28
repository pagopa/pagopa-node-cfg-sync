package it.gov.pagopa.node.cfgsync;

import it.gov.pagopa.node.cfgsync.controller.HomeController;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
class HomeControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HomeController homeController;

    @Test
    void homeSwaggerBasePathSlash() {
        ResponseEntity<String> response = restTemplate.exchange("/", HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertFalse(response.getBody().isEmpty());
        assertThat(response.getBody().contains("Swagger UI"));
    }

    @Test
    void homeSwaggerBasePathEmpty() {
        ReflectionTestUtils.setField(homeController, "basePath", "");

        ResponseEntity<String> response = restTemplate.exchange("/", HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertFalse(response.getBody().isEmpty());
        assertThat(response.getBody().contains("Swagger UI"));

        ReflectionTestUtils.setField(homeController, "basePath", "/");
    }
}
