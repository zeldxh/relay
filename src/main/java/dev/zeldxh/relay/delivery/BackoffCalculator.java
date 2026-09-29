package dev.zeldxh.relay.delivery;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BackoffCalculator {

    private final Duration base;
    private final Duration max;

    public BackoffCalculator() {
        this(Duration.ofSeconds(30), Duration.ofHours(1));
    }

    public BackoffCalculator(Duration base, Duration max) {
        this.base = base;
        this.max = max;
    }

    public Duration delayFor(int attemptCount) {
        int exponent = Math.min(Math.max(attemptCount, 0), 20);
        Duration delay = base.multipliedBy(1L << exponent);
        return delay.compareTo(max) > 0 ? max : delay;
    }
}
