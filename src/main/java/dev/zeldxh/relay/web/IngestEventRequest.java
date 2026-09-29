package dev.zeldxh.relay.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record IngestEventRequest(@NotNull JsonNode payload) {
}
