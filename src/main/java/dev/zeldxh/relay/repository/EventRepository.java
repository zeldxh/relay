package dev.zeldxh.relay.repository;

import dev.zeldxh.relay.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
