package dev.zeldxh.relay.repository;

import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Event;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.domain.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeliveryRepositoryTest {

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization organization() {
        return organizationRepository.save(new Organization("test-org", "test-hash-" + System.nanoTime()));
    }

    @Test
    void findsDueDeliveriesByStatusAndNextAttemptAt() {
        Organization organization = organization();
        Endpoint endpoint = endpointRepository.save(new Endpoint(organization, "orders-webhook", "https://example.com/hook", "s3cr3t"));
        Topic topic = topicRepository.save(new Topic(organization, "order.created"));
        Event event = eventRepository.save(new Event(topic, "{}"));

        Delivery due = new Delivery(event, endpoint);
        due.setNextAttemptAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        deliveryRepository.save(due);

        Delivery notYetDue = new Delivery(event, endpoint);
        notYetDue.setNextAttemptAt(Instant.now().plus(1, ChronoUnit.HOURS));
        deliveryRepository.save(notYetDue);

        List<Delivery> result = deliveryRepository.findByStatusAndNextAttemptAtLessThanEqual(DeliveryStatus.PENDING, Instant.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(due.getId());
    }

    @Test
    void findsByStatusPaginated() {
        Organization organization = organization();
        Endpoint endpoint = endpointRepository.save(new Endpoint(organization, "orders-webhook", "https://example.com/hook", "s3cr3t"));
        Topic topic = topicRepository.save(new Topic(organization, "order.created"));
        Event event = eventRepository.save(new Event(topic, "{}"));

        Delivery failed = new Delivery(event, endpoint);
        failed.setStatus(DeliveryStatus.FAILED);
        deliveryRepository.save(failed);

        Delivery pending = new Delivery(event, endpoint);
        deliveryRepository.save(pending);

        assertThat(deliveryRepository.findByStatus(DeliveryStatus.FAILED, PageRequest.of(0, 10)).getContent())
                .extracting(Delivery::getId)
                .containsExactly(failed.getId());
    }
}
