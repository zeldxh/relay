package dev.zeldxh.relay.web;

import dev.zeldxh.relay.support.UniqueRemoteAddr;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsOrganizationWithoutRequiringAnApiKey() throws Exception {
        mockMvc.perform(post("/organizations")
                        .with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("acme"))
                .andExpect(jsonPath("$.apiKey").isNotEmpty());
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/organizations")
                        .with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimitsRepeatedCreationFromTheSameAddress() throws Exception {
        var sameAddress = UniqueRemoteAddr.unique();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/organizations")
                            .with(sameAddress)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"acme-" + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/organizations")
                        .with(sameAddress)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"one-too-many\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
