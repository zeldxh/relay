package dev.zeldxh.relay.streams;

import dev.zeldxh.relay.delivery.DeliveryWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Subscribes to the delivery stream as part of a consumer group and dispatches each newly
 * published delivery straight to the worker, giving new events a fast path instead of waiting
 * for the scheduled poller.
 */
@Component
public class DeliveryStreamConsumer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStreamConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final DeliveryWorker deliveryWorker;
    private final String streamKey;
    private final String consumerGroup;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;

    public DeliveryStreamConsumer(StringRedisTemplate redisTemplate,
                                   DeliveryWorker deliveryWorker,
                                   @Value("${relay.streams.delivery-stream-key}") String streamKey,
                                   @Value("${relay.streams.consumer-group}") String consumerGroup) {
        this.redisTemplate = redisTemplate;
        this.deliveryWorker = deliveryWorker;
        this.streamKey = streamKey;
        this.consumerGroup = consumerGroup;
    }

    @Override
    public void afterPropertiesSet() {
        ensureConsumerGroupExists();

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        container = StreamMessageListenerContainer.create(
                Objects.requireNonNull(redisTemplate.getConnectionFactory()), options);

        subscription = container.receive(
                Consumer.from(consumerGroup, "relay-consumer-" + UUID.randomUUID()),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                this::handleMessage);

        container.start();
    }

    private void handleMessage(MapRecord<String, String, String> message) {
        try {
            String rawId = message.getValue().get("deliveryId");
            if (rawId != null) {
                deliveryWorker.attemptById(Long.valueOf(rawId));
            }
        } catch (Exception e) {
            log.warn("Failed to process delivery stream message {}: {}", message.getId(), e.getMessage());
        } finally {
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
        }
    }

    private void ensureConsumerGroupExists() {
        // the stream must exist before a consumer group can be created against it
        redisTemplate.opsForStream().add(streamKey, Map.of("_init", "true"));
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
        } catch (Exception e) {
            // group already exists from a previous startup — fine, this is idempotent setup
        }
    }

    @Override
    public void destroy() {
        if (subscription != null) {
            subscription.cancel();
        }
        if (container != null) {
            container.stop();
        }
    }
}
