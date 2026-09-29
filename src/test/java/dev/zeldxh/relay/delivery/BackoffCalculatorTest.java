package dev.zeldxh.relay.delivery;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffCalculatorTest {

    private final BackoffCalculator calculator = new BackoffCalculator(Duration.ofSeconds(30), Duration.ofHours(1));

    @Test
    void increasesExponentiallyWithAttemptCount() {
        Duration first = calculator.delayFor(1);
        Duration second = calculator.delayFor(2);
        Duration third = calculator.delayFor(3);

        assertThat(first).isEqualTo(Duration.ofSeconds(60));
        assertThat(second).isEqualTo(Duration.ofSeconds(120));
        assertThat(third).isEqualTo(Duration.ofSeconds(240));
    }

    @Test
    void neverExceedsConfiguredMaximum() {
        Duration delay = calculator.delayFor(50);

        assertThat(delay).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void treatsNonPositiveAttemptsAsBaseDelay() {
        assertThat(calculator.delayFor(0)).isEqualTo(Duration.ofSeconds(30));
        assertThat(calculator.delayFor(-5)).isEqualTo(Duration.ofSeconds(30));
    }
}
