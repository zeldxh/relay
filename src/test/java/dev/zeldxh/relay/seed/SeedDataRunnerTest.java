package dev.zeldxh.relay.seed;

import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.repository.SubscriptionRepository;
import dev.zeldxh.relay.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("seed")
// This activates a second, separately-cached Spring context (distinct from the one every
// other @SpringBootTest shares) - including its own DeliveryStreamConsumer. Without its own
// stream key/group, that consumer would sit in the same Redis consumer group as the shared
// context's, racing it for messages neither one "owns" and silently stealing deliveries
// other tests are asserting against.
@TestPropertySource(properties = {
        "relay.streams.delivery-stream-key=relay:deliveries:seed-test",
        "relay.streams.consumer-group=relay-workers-seed-test"
})
class SeedDataRunnerTest {

    @Autowired
    private SeedDataRunner seedDataRunner;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    // The runner already ran once as part of context startup (it's an ApplicationRunner);
    // these assertions exercise that real run rather than invoking it again themselves.

    @Test
    void seedsTwoOrganizationsWithDashboardCredentials() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();
        Organization globex = organizationRepository.findByEmail("owner@globex.dev").orElseThrow();

        assertThat(acme.getName()).isEqualTo("Acme Corp");
        assertThat(acme.hasDashboardCredentials()).isTrue();
        assertThat(globex.getName()).isEqualTo("Globex Corp");
        assertThat(globex.hasDashboardCredentials()).isTrue();
    }

    @Test
    void acmeHasEndpointsInBothVerificationStates() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();

        var endpoints = endpointRepository.findByOrganization(acme);

        assertThat(endpoints).hasSize(3);
        assertThat(endpoints).filteredOn(e -> e.isVerified()).hasSize(2);
        assertThat(endpoints).filteredOn(e -> !e.isVerified()).hasSize(1);
    }

    @Test
    void acmeHasTopicsAndSubscriptions() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();

        assertThat(topicRepository.findByOrganization(acme)).hasSize(3);

        var orderCreated = topicRepository.findByOrganization(acme).stream()
                .filter(t -> t.getName().equals("order.created"))
                .findFirst().orElseThrow();
        assertThat(subscriptionRepository.findByTopic(orderCreated)).hasSize(1);
    }

    @Test
    void deliveriesCoverAllThreeStatuses() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();

        var deliveries = deliveryRepository
                .findByEndpoint_Organization(acme, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertThat(deliveries).extracting(d -> d.getStatus())
                .contains(DeliveryStatus.SUCCESS, DeliveryStatus.FAILED, DeliveryStatus.PENDING);
    }

    @Test
    void runningAgainIsIdempotent() {
        long organizationsBefore = organizationRepository.count();

        seedDataRunner.run(null);

        assertThat(organizationRepository.count()).isEqualTo(organizationsBefore);
    }
}
