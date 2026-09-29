package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.Event;

import java.util.List;

public record EventResponse(Long eventId, List<Long> deliveryIds) {
    public static EventResponse from(Event event, List<Delivery> deliveries) {
        return new EventResponse(event.getId(), deliveries.stream().map(Delivery::getId).toList());
    }
}
