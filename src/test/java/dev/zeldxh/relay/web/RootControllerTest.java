package dev.zeldxh.relay.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootReturnsJsonInfoWithoutRequiringAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.name").value("relay"))
                .andExpect(jsonPath("$.docs").value("http://localhost:3001"))
                .andExpect(jsonPath("$.dashboard").value("http://localhost:3000"))
                .andExpect(jsonPath("$.health").value("/actuator/health"));
    }
}
