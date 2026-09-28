package dev.zeldxh.relay.repository;

import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    Optional<Endpoint> findByIdAndOrganization(Long id, Organization organization);

    List<Endpoint> findByOrganization(Organization organization);
}
