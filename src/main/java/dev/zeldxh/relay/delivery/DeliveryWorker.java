package dev.zeldxh.relay.delivery;

import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Event;
import dev.zeldxh.relay.ratelimit.RateLimiterService;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.signing.HmacSigner;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private final DeliveryRepository deliveryRepository;
    private final HmacSigner hmacSigner;
    private final RestClient restClient;
    private final BackoffCalculator backoffCalculator;
    private final RateLimiterService rateLimiterService;
    private final MeterRegistry meterRegistry;

    public DeliveryWorker(DeliveryRepository deliveryRepository, HmacSigner hmacSigner,
                           RestClient restClient, BackoffCalculator backoffCalculator,
                           RateLimiterService rateLimiterService, MeterRegistry meterRegistry) {
        this.deliveryRepository = deliveryRepository;
        this.hmacSigner = hmacSigner;
        this.restClient = restClient;
        this.backoffCalculator = backoffCalculator;
        this.rateLimiterService = rateLimiterService;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${relay.worker.poll-interval-ms:5000}")
    @Transactional
    public void processDueDeliveries() {
        List<Delivery> due = deliveryRepository.findByStatusAndNextAttemptAtLessThanEqual(DeliveryStatus.PENDING, Instant.now());
        due.forEach(this::attempt);
    }

    @Transactional
    public void attemptById(Long deliveryId) {
        deliveryRepository.findById(deliveryId).ifPresent(this::attempt);
    }

    @Transactional
    public void attempt(Delivery delivery) {
        Event event = delivery.getEvent();
        Endpoint endpoint = delivery.getEndpoint();

        if (!rateLimiterService.tryConsume(endpoint.getId(), endpoint.getRateLimitPerSecond())) {
            meterRegistry.counter("relay.delivery.attempts", "outcome", "rate_limited").increment();
            delivery.setNextAttemptAt(Instant.now().plusSeconds(1));
            deliveryRepository.save(delivery);
            return;
        }

        String payload = event.getPayload();
        String signature = hmacSigner.sign(payload, endpoint.getSecret());

        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(Instant.now());

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(endpoint.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HmacSigner.SIGNATURE_HEADER, signature)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            delivery.setLastResponseStatus(response.getStatusCode().value());
            delivery.setStatus(DeliveryStatus.SUCCESS);
            meterRegistry.counter("relay.delivery.attempts", "outcome", "success").increment();
        } catch (RestClientResponseException e) {
            delivery.setLastResponseStatus(e.getStatusCode().value());
            scheduleRetryOrFail(delivery);
            meterRegistry.counter("relay.delivery.attempts", "outcome", "failure").increment();
        } catch (RestClientException e) {
            log.warn("Delivery {} attempt {} failed: {}", delivery.getId(), delivery.getAttemptCount(), e.getMessage());
            delivery.setLastResponseStatus(null);
            scheduleRetryOrFail(delivery);
            meterRegistry.counter("relay.delivery.attempts", "outcome", "failure").increment();
        } finally {
            sample.stop(meterRegistry.timer("relay.delivery.duration"));
        }

        deliveryRepository.save(delivery);
    }

    private void scheduleRetryOrFail(Delivery delivery) {
        if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
            delivery.setStatus(DeliveryStatus.FAILED);
        } else {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextAttemptAt(Instant.now().plus(backoffCalculator.delayFor(delivery.getAttemptCount())));
        }
    }
}
