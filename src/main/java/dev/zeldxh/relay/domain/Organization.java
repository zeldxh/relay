package dev.zeldxh.relay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "api_key_hash", nullable = false, unique = true)
    private String apiKeyHash;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Organization() {
    }

    public Organization(String name, String apiKeyHash) {
        this.name = name;
        this.apiKeyHash = apiKeyHash;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean hasDashboardCredentials() {
        return email != null && passwordHash != null;
    }

    public void setDashboardCredentials(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
