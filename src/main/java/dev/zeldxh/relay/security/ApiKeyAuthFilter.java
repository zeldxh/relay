package dev.zeldxh.relay.security;

import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.session.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Accepts either a {@code relay_} API key (the bearer credential for the public delivery API)
 * or a dashboard session cookie (set by {@link dev.zeldxh.relay.web.AuthController} after
 * login), so the browser dashboard never needs to hold the organization's API key at all.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String ORGANIZATION_ATTRIBUTE = "currentOrganization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of("/", "/organizations");

    private final OrganizationRepository organizationRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final SessionService sessionService;

    public ApiKeyAuthFilter(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher,
                             SessionService sessionService) {
        this.organizationRepository = organizationRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.sessionService = sessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.contains(request.getRequestURI())
                || request.getRequestURI().startsWith("/actuator")
                || request.getRequestURI().startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<Organization> organization = resolveByApiKey(request).or(() -> resolveBySessionCookie(request));
        if (organization.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing or invalid credentials");
            return;
        }

        request.setAttribute(ORGANIZATION_ATTRIBUTE, organization.get());
        filterChain.doFilter(request, response);
    }

    private Optional<Organization> resolveByApiKey(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String rawKey = header.substring(BEARER_PREFIX.length());
        return organizationRepository.findByApiKeyHash(apiKeyHasher.hash(rawKey));
    }

    private Optional<Organization> resolveBySessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (SessionService.COOKIE_NAME.equals(cookie.getName())) {
                return sessionService.resolve(cookie.getValue()).flatMap(organizationRepository::findById);
            }
        }
        return Optional.empty();
    }
}
