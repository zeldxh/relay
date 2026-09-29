package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.repository.EndpointRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import static com.jayway.jsonpath.JsonPath.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    @Autowired
    private EndpointRepository endpointRepository;

    private OrganizationFixtures.Seeded org() {
        return OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
    }

    private Endpoint verifiedEndpoint(OrganizationFixtures.Seeded org, String name, String url, String secret) {
        Endpoint endpoint = new Endpoint(org.organization(), name, url, secret);
        endpoint.markVerified();
        return endpointRepository.save(endpoint);
    }

    private Long createTopic(String authHeader, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/topics")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void ingestingWithNoSubscribersCreatesEventButNoDeliveries() throws Exception {
        OrganizationFixtures.Seeded org = org();
        Long topicId = createTopic(org.authorizationHeader(), "order.created");

        mockMvc.perform(post("/topics/" + topicId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{\"orderId\":1}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.deliveryIds.length()").value(0));
    }

    @Test
    void ingestingFansOutToEverySubscribedEndpoint() throws Exception {
        OrganizationFixtures.Seeded org = org();
        Long topicId = createTopic(org.authorizationHeader(), "order.created");

        Endpoint endpointA = verifiedEndpoint(org, "a", "https://example.com/a", "secretA");
        Endpoint endpointB = verifiedEndpoint(org, "b", "https://example.com/b", "secretB");

        mockMvc.perform(post("/topics/" + topicId + "/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpointId\":" + endpointA.getId() + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/topics/" + topicId + "/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpointId\":" + endpointB.getId() + "}"))
                .andExpect(status().isCreated());

        MvcResult ingest = mockMvc.perform(post("/topics/" + topicId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{\"orderId\":1}}"))
                .andExpect(status().isCreated())
                .andReturn();

        java.util.List<Integer> deliveryIds = read(ingest.getResponse().getContentAsString(), "$.deliveryIds");
        assertThat(deliveryIds).hasSize(2);
    }

    @Test
    void cannotSubscribeAnotherOrganizationsEndpoint() throws Exception {
        OrganizationFixtures.Seeded owner = org();
        OrganizationFixtures.Seeded other = org();
        Long topicId = createTopic(owner.authorizationHeader(), "order.created");
        Endpoint othersEndpoint = endpointRepository.save(new Endpoint(other.organization(), "b", "https://example.com/b", "secretB"));

        mockMvc.perform(post("/topics/" + topicId + "/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, owner.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpointId\":" + othersEndpoint.getId() + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotSubscribeAnUnverifiedEndpoint() throws Exception {
        OrganizationFixtures.Seeded org = org();
        Long topicId = createTopic(org.authorizationHeader(), "order.created");
        Endpoint unverified = endpointRepository.save(new Endpoint(org.organization(), "a", "https://example.com/a", "secretA"));

        mockMvc.perform(post("/topics/" + topicId + "/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpointId\":" + unverified.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundForUnknownTopic() throws Exception {
        OrganizationFixtures.Seeded org = org();

        mockMvc.perform(post("/topics/999999/events")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsTopicsAndSubscriptionsForTheCallingOrganization() throws Exception {
        OrganizationFixtures.Seeded org = org();
        Long topicId = createTopic(org.authorizationHeader(), "order.created");
        Endpoint endpoint = verifiedEndpoint(org, "a", "https://example.com/a", "secretA");

        mockMvc.perform(post("/topics/" + topicId + "/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpointId\":" + endpoint.getId() + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/topics").header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("order.created"));

        mockMvc.perform(get("/topics/" + topicId + "/subscriptions").header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].endpointId").value(endpoint.getId()));
    }
}
