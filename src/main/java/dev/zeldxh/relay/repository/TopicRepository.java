package dev.zeldxh.relay.repository;

import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByIdAndOrganization(Long id, Organization organization);

    List<Topic> findByOrganization(Organization organization);
}
