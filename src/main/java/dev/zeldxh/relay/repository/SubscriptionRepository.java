package dev.zeldxh.relay.repository;

import dev.zeldxh.relay.domain.Subscription;
import dev.zeldxh.relay.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByTopic(Topic topic);
}
