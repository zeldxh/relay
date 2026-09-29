package dev.zeldxh.relay.integration;

import dev.zeldxh.relay.delivery.DeliveryWorker;
import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.signing.HmacSigner;
import dev.zeldxh.relay.support.UniqueRemoteAddr;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.jsonpath.JsonPath.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full webhook lifecycle through the real REST API, the Redis stream fast path,
 * and the delivery worker: register an endpoint, ingest an event, let it get delivered
 * automatically, and confirm it shows up signed and correctly in the dashboard.
 *
 * Deliberately not @Transactional: the stream consumer runs on its own thread with its own
 * connection, so it needs to see committed data, not whatever's still open in the test's
 * transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookDeliveryFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliveryWorker worker;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private HmacSigner hmacSigner;

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

    private String createOrganizationAndGetAuthHeader() throws Exception {
        MvcResult createOrg = mockMvc.perform(post("/organizations")
                        .with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String apiKey = read(createOrg.getResponse().getContentAsString(), "$.apiKey");
        return "Bearer " + apiKey;
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

    private void verify(String authHeader, Long endpointId) throws Exception {
        mockMvc.perform(post("/endpoints/" + endpointId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk());
    }

    private void markVerifiedDirectly(Long endpointId) {
        Endpoint endpoint = endpointRepository.findById(endpointId).orElseThrow();
        endpoint.markVerified();
        endpointRepository.save(endpoint);
    }

    private void subscribe(String authHeader, Long topicId, Long endpointId) throws Exception {
        mockMvc.perform(post("/topics/" + topicId + "/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpointId\":" + endpointId + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deliversIngestedEventAutomaticallyViaTheStreamAndShowsUpInDashboard() throws Exception {
        String authHeader = createOrganizationAndGetAuthHeader();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedSignature = new AtomicReference<>();
        server.createContext("/hook", exchange -> {
            receivedSignature.set(exchange.getRequestHeaders().getFirst(HmacSigner.SIGNATURE_HEADER));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        MvcResult createEndpoint = mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders\",\"url\":\"http://localhost:" + port + "/hook\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String endpointBody = createEndpoint.getResponse().getContentAsString();
        Long endpointId = ((Number) read(endpointBody, "$.id")).longValue();
        String secret = read(endpointBody, "$.secret");

        verify(authHeader, endpointId);
        Long topicId = createTopic(authHeader, "order.created");
        subscribe(authHeader, topicId, endpointId);

        MvcResult ingest = mockMvc.perform(post("/topics/" + topicId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{\"orderId\":42}}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long deliveryId = ((Number) ((java.util.List<?>) read(ingest.getResponse().getContentAsString(), "$.deliveryIds")).get(0)).longValue();

        // no manual trigger here — the stream consumer should pick this up and deliver it on its own
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        });

        String expectedPayload = "{\"orderId\":42}";
        assertThat(receivedBody.get()).isEqualTo(expectedPayload);
        assertThat(receivedSignature.get()).isEqualTo(hmacSigner.sign(expectedPayload, secret));

        mockMvc.perform(get("/deliveries")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + deliveryId + ")]").exists());
    }

    @Test
    void replayedFailedDeliveryBecomesDueAgain() throws Exception {
        String authHeader = createOrganizationAndGetAuthHeader();
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        MvcResult createEndpoint = mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders\",\"url\":\"http://localhost:" + port + "/hook\"}"))
                .andReturn();
        Long endpointId = ((Number) read(createEndpoint.getResponse().getContentAsString(), "$.id")).longValue();

        // this endpoint's /hook always returns 500, so verify would fail too — bypass directly
        markVerifiedDirectly(endpointId);
        Long topicId = createTopic(authHeader, "order.created");
        subscribe(authHeader, topicId, endpointId);

        MvcResult ingest = mockMvc.perform(post("/topics/" + topicId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{}}"))
                .andReturn();
        Long deliveryId = ((Number) ((java.util.List<?>) read(ingest.getResponse().getContentAsString(), "$.deliveryIds")).get(0)).longValue();

        // wait for the stream consumer's first (failing) attempt before forcing it near exhaustion
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
            assertThat(delivery.getAttemptCount()).isGreaterThanOrEqualTo(1);
        });

        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        delivery.setAttemptCount(delivery.getMaxAttempts() - 1);
        delivery.setNextAttemptAt(Instant.now());
        deliveryRepository.save(delivery);
        worker.attemptById(deliveryId);

        Delivery failed = deliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.FAILED);

        mockMvc.perform(post("/deliveries/" + deliveryId + "/replay")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(0));
    }
}
