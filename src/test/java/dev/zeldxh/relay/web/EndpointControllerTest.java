package dev.zeldxh.relay.web;

import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.support.OrganizationFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    private String authHeader() {
        return OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org").authorizationHeader();
    }

    @Test
    void createsEndpointWithGeneratedSecret() throws Exception {
        mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders-webhook\",\"url\":\"https://example.com/hook\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("orders-webhook"))
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.rateLimitPerSecond").value(10));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"url\":\"https://example.com/hook\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingEndpoint() throws Exception {
        mockMvc.perform(get("/endpoints/999999")
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsRequestsWithoutApiKey() throws Exception {
        mockMvc.perform(post("/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders-webhook\",\"url\":\"https://example.com/hook\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestsWithInvalidApiKey() throws Exception {
        mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders-webhook\",\"url\":\"https://example.com/hook\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotAccessAnotherOrganizationsEndpoint() throws Exception {
        String ownerAuth = authHeader();
        String createBody = mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders-webhook\",\"url\":\"https://example.com/hook\"}"))
                .andReturn().getResponse().getContentAsString();
        Long endpointId = ((Number) com.jayway.jsonpath.JsonPath.read(createBody, "$.id")).longValue();

        mockMvc.perform(get("/endpoints/" + endpointId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsOnlyTheCallingOrganizationsEndpoints() throws Exception {
        String ownerAuth = authHeader();
        mockMvc.perform(post("/endpoints")
                .header(HttpHeaders.AUTHORIZATION, ownerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"orders-webhook\",\"url\":\"https://example.com/hook\"}"));

        String otherAuth = authHeader();
        mockMvc.perform(post("/endpoints")
                .header(HttpHeaders.AUTHORIZATION, otherAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"other-webhook\",\"url\":\"https://example.com/other\"}"));

        mockMvc.perform(get("/endpoints").header(HttpHeaders.AUTHORIZATION, ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("orders-webhook"));
    }
}
