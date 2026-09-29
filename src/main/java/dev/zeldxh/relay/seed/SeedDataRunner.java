package dev.zeldxh.relay.seed;

import dev.zeldxh.relay.domain.AuditLog;
import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Event;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.domain.Subscription;
import dev.zeldxh.relay.domain.Topic;
import dev.zeldxh.relay.repository.AuditLogRepository;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.repository.EventRepository;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.repository.SubscriptionRepository;
import dev.zeldxh.relay.repository.TopicRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.security.PasswordHasher;
import dev.zeldxh.relay.signing.HmacSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Populates the database with realistic sample data for local exploration - a couple of
 * organizations, endpoints in both verified states, topics, subscriptions, and deliveries
 * covering all three statuses. Only runs under the "seed" profile, so it's never active in
 * tests or a normal dev/prod run:
 *
 * <pre>SPRING_PROFILES_ACTIVE=seed ./mvnw spring-boot:run</pre>
 *
 * Idempotent: if the marker organization already exists (e.g. you restarted with the
 * profile still active), it logs and does nothing rather than failing on unique constraints.
 */
@Component
@Profile("seed")
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final String MARKER_EMAIL = "owner@acme.dev";
    private static final String DEMO_PASSWORD = "demo-password-123";

    private final OrganizationRepository organizationRepository;
    private final EndpointRepository endpointRepository;
    private final TopicRepository topicRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final PasswordHasher passwordHasher;
    private final HmacSigner hmacSigner;

    public SeedDataRunner(OrganizationRepository organizationRepository, EndpointRepository endpointRepository,
                           TopicRepository topicRepository, SubscriptionRepository subscriptionRepository,
                           EventRepository eventRepository, DeliveryRepository deliveryRepository,
                           AuditLogRepository auditLogRepository, ApiKeyHasher apiKeyHasher,
                           PasswordHasher passwordHasher, HmacSigner hmacSigner) {
        this.organizationRepository = organizationRepository;
        this.endpointRepository = endpointRepository;
        this.topicRepository = topicRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.auditLogRepository = auditLogRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.passwordHasher = passwordHasher;
        this.hmacSigner = hmacSigner;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (organizationRepository.existsByEmail(MARKER_EMAIL)) {
            log.info("Seed data already present (found organization with email {}) - skipping.", MARKER_EMAIL);
            return;
        }

        String acmeApiKey = seedAcme();
        String globexApiKey = seedGlobex();

        log.info("""

                ================================================================
                 Seed data loaded. Log in to the dashboard or use these directly:

                 Acme Corp     email={}  password={}
                               apiKey={}

                 Globex Corp   email={}  password={}
                               apiKey={}
                ================================================================
                """,
                MARKER_EMAIL, DEMO_PASSWORD, acmeApiKey,
                "owner@globex.dev", DEMO_PASSWORD, globexApiKey);
    }

    private String seedAcme() {
        String apiKey = apiKeyHasher.generateKey();
        Organization acme = new Organization("Acme Corp", apiKeyHasher.hash(apiKey));
        acme.setDashboardCredentials(MARKER_EMAIL, passwordHasher.hash(DEMO_PASSWORD));
        acme = organizationRepository.save(acme);
        audit(acme, "organization.signed_up", "email=" + MARKER_EMAIL);

        Endpoint orders = verifiedEndpoint(acme, "orders-webhook", "https://example.com/hooks/orders");
        Endpoint inventory = verifiedEndpoint(acme, "inventory-webhook", "https://example.com/hooks/inventory");
        unverifiedEndpoint(acme, "legacy-webhook", "https://example.com/hooks/legacy");

        Topic orderCreated = topic(acme, "order.created");
        Topic orderRefunded = topic(acme, "order.refunded");
        Topic inventoryUpdated = topic(acme, "inventory.updated");

        subscribe(orderCreated, orders);
        subscribe(orderRefunded, orders);
        subscribe(inventoryUpdated, inventory);

        successfulDelivery(orderCreated, orders, "{\"orderId\":1001,\"total\":49.99}", 200);
        failedDelivery(orderCreated, orders, "{\"orderId\":1002,\"total\":19.99}", 500);
        pendingRetryDelivery(orderRefunded, orders, "{\"orderId\":1001,\"reason\":\"customer request\"}", 503);
        successfulDelivery(inventoryUpdated, inventory, "{\"sku\":\"WIDGET-1\",\"quantity\":42}", 200);

        return apiKey;
    }

    private String seedGlobex() {
        String apiKey = apiKeyHasher.generateKey();
        Organization globex = new Organization("Globex Corp", apiKeyHasher.hash(apiKey));
        globex.setDashboardCredentials("owner@globex.dev", passwordHasher.hash(DEMO_PASSWORD));
        globex = organizationRepository.save(globex);
        audit(globex, "organization.signed_up", "email=owner@globex.dev");

        Endpoint webhook = verifiedEndpoint(globex, "payments-webhook", "https://example.com/hooks/globex");
        Topic paymentProcessed = topic(globex, "payment.processed");
        subscribe(paymentProcessed, webhook);
        successfulDelivery(paymentProcessed, webhook, "{\"paymentId\":\"pay_1\",\"amount\":250.00}", 200);

        return apiKey;
    }

    private Endpoint verifiedEndpoint(Organization organization, String name, String url) {
        Endpoint endpoint = new Endpoint(organization, name, url, hmacSigner.generateSecret());
        endpoint.markVerified();
        endpoint = endpointRepository.save(endpoint);
        audit(organization, "endpoint.created", "name=" + name + ", url=" + url);
        audit(organization, "endpoint.verified", "endpointId=" + endpoint.getId());
        return endpoint;
    }

    private void unverifiedEndpoint(Organization organization, String name, String url) {
        Endpoint endpoint = new Endpoint(organization, name, url, hmacSigner.generateSecret());
        endpointRepository.save(endpoint);
        audit(organization, "endpoint.created", "name=" + name + ", url=" + url);
    }

    private Topic topic(Organization organization, String name) {
        Topic topic = topicRepository.save(new Topic(organization, name));
        audit(organization, "topic.created", "name=" + name);
        return topic;
    }

    private void subscribe(Topic topic, Endpoint endpoint) {
        subscriptionRepository.save(new Subscription(topic, endpoint));
        audit(topic.getOrganization(), "subscription.created",
                "topic=" + topic.getName() + ", endpointId=" + endpoint.getId());
    }

    private void successfulDelivery(Topic topic, Endpoint endpoint, String payload, int responseStatus) {
        Event event = eventRepository.save(new Event(topic, payload));
        Delivery delivery = new Delivery(event, endpoint);
        delivery.setStatus(DeliveryStatus.SUCCESS);
        delivery.setAttemptCount(1);
        delivery.setLastAttemptAt(Instant.now().minus(1, ChronoUnit.HOURS));
        delivery.setLastResponseStatus(responseStatus);
        deliveryRepository.save(delivery);
    }

    private void failedDelivery(Topic topic, Endpoint endpoint, String payload, int responseStatus) {
        Event event = eventRepository.save(new Event(topic, payload));
        Delivery delivery = new Delivery(event, endpoint);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setAttemptCount(delivery.getMaxAttempts());
        delivery.setLastAttemptAt(Instant.now().minus(2, ChronoUnit.HOURS));
        delivery.setLastResponseStatus(responseStatus);
        deliveryRepository.save(delivery);
    }

    /** Left genuinely PENDING so the real worker picks it up and retries it once this starts. */
    private void pendingRetryDelivery(Topic topic, Endpoint endpoint, String payload, int responseStatus) {
        Event event = eventRepository.save(new Event(topic, payload));
        Delivery delivery = new Delivery(event, endpoint);
        delivery.setAttemptCount(2);
        delivery.setLastAttemptAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        delivery.setLastResponseStatus(responseStatus);
        delivery.setNextAttemptAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        deliveryRepository.save(delivery);
    }

    private void audit(Organization organization, String action, String details) {
        auditLogRepository.save(new AuditLog(organization, action, details));
    }
}
