package dev.zeldxh.relay.streams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes newly-created deliveries to Redis so the consumer can dispatch them immediately,
 * instead of waiting for the next scheduled DB poll. Retries still go through the poller,
 * since they're inherently delayed work that a plain stream isn't a good fit for.
 */
@Component
public class DeliveryStreamPublisher {

    private final StringRedisTemplate redisTemplate;
    private final String streamKey;

    public DeliveryStreamPublisher(StringRedisTemplate redisTemplate,
                                    @Value("${relay.streams.delivery-stream-key}") String streamKey) {
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
    }

    public void publish(Long deliveryId) {
        redisTemplate.opsForStream().add(streamKey, Map.of("deliveryId", deliveryId.toString()));
    }
}
