package dev.zeldxh.relay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 8;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_response_status")
    private Integer lastResponseStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Delivery() {
    }

    public Delivery(Event event, Endpoint endpoint) {
        this.event = event;
        this.endpoint = endpoint;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public Integer getLastResponseStatus() {
        return lastResponseStatus;
    }

    public void setLastResponseStatus(Integer lastResponseStatus) {
        this.lastResponseStatus = lastResponseStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
