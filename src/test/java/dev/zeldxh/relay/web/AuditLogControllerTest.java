package dev.zeldxh.relay.web;

import dev.zeldxh.relay.support.UniqueRemoteAddr;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recordsAndListsActionsForTheCallingOrganization() throws Exception {
        MvcResult createOrg = mockMvc.perform(post("/organizations")
                        .with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String apiKey = read(createOrg.getResponse().getContentAsString(), "$.apiKey");
        String authHeader = "Bearer " + apiKey;

        mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders\",\"url\":\"https://example.com/hook\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/audit-logs").header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'organization.created')]").exists())
                .andExpect(jsonPath("$.content[?(@.action == 'endpoint.created')]").exists());
    }

    @Test
    void rejectsListingWithoutApiKey() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isUnauthorized());
    }
}
