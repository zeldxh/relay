package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Topic;

import java.time.Instant;

public record TopicResponse(Long id, String name, Instant createdAt) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(topic.getId(), topic.getName(), topic.getCreatedAt());
    }
}
