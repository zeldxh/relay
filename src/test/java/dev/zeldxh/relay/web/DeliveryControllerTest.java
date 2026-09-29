package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Event;
import dev.zeldxh.relay.domain.Topic;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.repository.EventRepository;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.repository.TopicRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.support.OrganizationFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    @Autowired
    private TopicRepository topicRepository;

    private Delivery seed(OrganizationFixtures.Seeded org, DeliveryStatus status) {
        Endpoint endpoint = endpointRepository.save(new Endpoint(org.organization(), "orders-webhook", "https://example.com/hook", "s3cr3t"));
        Topic topic = topicRepository.save(new Topic(org.organization(), "order.created." + System.nanoTime()));
        Event event = eventRepository.save(new Event(topic, "{}"));
        Delivery delivery = new Delivery(event, endpoint);
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
    }

    @Test
    void listsDeliveriesFilteredByStatus() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        seed(org, DeliveryStatus.SUCCESS);
        Delivery failed = seed(org, DeliveryStatus.FAILED);

        mockMvc.perform(get("/deliveries")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(failed.getId()))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    void replayResetsFailedDeliveryToPending() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        Delivery failed = seed(org, DeliveryStatus.FAILED);
        failed.setAttemptCount(failed.getMaxAttempts());
        failed.setNextAttemptAt(Instant.now().plus(1, ChronoUnit.HOURS));
        deliveryRepository.save(failed);

        mockMvc.perform(post("/deliveries/" + failed.getId() + "/replay")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(0));
    }

    @Test
    void rejectsReplayOfNonFailedDelivery() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        Delivery pending = seed(org, DeliveryStatus.PENDING);

        mockMvc.perform(post("/deliveries/" + pending.getId() + "/replay")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundForUnknownDelivery() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");

        mockMvc.perform(post("/deliveries/999999/replay")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotReplayAnotherOrganizationsDelivery() throws Exception {
        OrganizationFixtures.Seeded owner = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "owner-org");
        OrganizationFixtures.Seeded other = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "other-org");
        Delivery failed = seed(owner, DeliveryStatus.FAILED);

        mockMvc.perform(post("/deliveries/" + failed.getId() + "/replay")
                        .header(HttpHeaders.AUTHORIZATION, other.authorizationHeader()))
                .andExpect(status().isNotFound());
    }
}
