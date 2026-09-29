package dev.zeldxh.relay.delivery;

import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Event;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.domain.Topic;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.repository.EventRepository;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.repository.TopicRepository;
import dev.zeldxh.relay.signing.HmacSigner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliveryWorkerTest {

    @Autowired
    private DeliveryWorker worker;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private HmacSigner hmacSigner;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TopicRepository topicRepository;

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private Organization organization() {
        return organizationRepository.save(new Organization("test-org", "test-hash-" + System.nanoTime()));
    }

    private Delivery seedDelivery(String path, String secret) {
        Organization organization = organization();
        Endpoint endpoint = endpointRepository.save(new Endpoint(organization, "test", "http://localhost:" + port + path, secret));
        Topic topic = topicRepository.save(new Topic(organization, "test.event"));
        Event event = eventRepository.save(new Event(topic, "{\"a\":1}"));
        return deliveryRepository.save(new Delivery(event, endpoint));
    }

    private Delivery seedDeliveryWithRateLimit(String path, String secret, int ratePerSecond) {
        Organization organization = organization();
        Endpoint endpoint = new Endpoint(organization, "test", "http://localhost:" + port + path, secret);
        endpoint.setRateLimitPerSecond(ratePerSecond);
        endpoint = endpointRepository.save(endpoint);
        Topic topic = topicRepository.save(new Topic(organization, "test.event"));
        Event event = eventRepository.save(new Event(topic, "{\"a\":1}"));
        return deliveryRepository.save(new Delivery(event, endpoint));
    }

    @Test
    void marksDeliverySuccessAndSendsValidSignatureWhenEndpointReturns2xx() throws Exception {
        AtomicReference<String> receivedSignature = new AtomicReference<>();
        server.createContext("/hook", exchange -> {
            receivedSignature.set(exchange.getRequestHeaders().getFirst(HmacSigner.SIGNATURE_HEADER));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDelivery("/hook", "s3cr3t");
        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(reloaded.getLastResponseStatus()).isEqualTo(200);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(receivedSignature.get()).isEqualTo(hmacSigner.sign("{\"a\":1}", "s3cr3t"));
    }

    @Test
    void schedulesRetryWithBackoffWhenEndpointReturns5xx() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDelivery("/hook", "s3cr3t");
        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getLastResponseStatus()).isEqualTo(500);
        assertThat(reloaded.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void marksDeliveryFailedAfterExhaustingMaxAttempts() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDelivery("/hook", "s3cr3t");
        delivery.setAttemptCount(delivery.getMaxAttempts() - 1);
        deliveryRepository.save(delivery);

        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(reloaded.getAttemptCount()).isEqualTo(reloaded.getMaxAttempts());
    }

    @Test
    void schedulesRetryWhenEndpointIsUnreachable() {
        // server is deliberately never started, so the port is not listening
        Delivery delivery = seedDelivery("/hook", "s3cr3t");

        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.getLastResponseStatus()).isNull();
    }

    @Test
    void skipsAttemptWithoutHittingEndpointWhenRateLimited() throws Exception {
        AtomicReference<Integer> requestCount = new AtomicReference<>(0);
        server.createContext("/hook", exchange -> {
            requestCount.updateAndGet(count -> count + 1);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDeliveryWithRateLimit("/hook", "s3cr3t", 1);

        worker.attempt(delivery); // consumes the single token for this endpoint, succeeds
        worker.attempt(delivery); // should be rate-limited, no HTTP call made

        assertThat(requestCount.get()).isEqualTo(1);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
    }
}
