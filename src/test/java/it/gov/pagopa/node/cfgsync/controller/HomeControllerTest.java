package it.gov.pagopa.node.cfgsync.controller;

import it.gov.pagopa.node.cfgsync.config.ViewNamingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewNamingStrategy viewNamingStrategy;

    @Test
    void home() throws Exception {
        String url = "/";
        mockMvc.perform(get(url)).andExpect(status().is3xxRedirection());
    }

    @Test
    void info() throws Exception {
        String url = "/info";
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }
}
