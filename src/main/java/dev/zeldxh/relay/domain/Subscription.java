package dev.zeldxh.relay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Links an endpoint to a topic: this endpoint receives every event published to this topic.
 */
@Entity
@Table(name = "subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "endpoint_id"}))
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Subscription() {
    }

    public Subscription(Topic topic, Endpoint endpoint) {
        this.topic = topic;
        this.endpoint = endpoint;
    }

    public Long getId() {
        return id;
    }

    public Topic getTopic() {
        return topic;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
