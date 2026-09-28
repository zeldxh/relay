package dev.zeldxh.relay.web;

import dev.zeldxh.relay.audit.AuditLogService;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.ratelimit.PublicEndpointRateLimiter;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.security.PasswordHasher;
import dev.zeldxh.relay.session.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OrganizationRepository organizationRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final PasswordHasher passwordHasher;
    private final SessionService sessionService;
    private final AuditLogService auditLogService;
    private final PublicEndpointRateLimiter publicEndpointRateLimiter;
    private final boolean secureCookie;

    public AuthController(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher,
                           PasswordHasher passwordHasher, SessionService sessionService,
                           AuditLogService auditLogService, PublicEndpointRateLimiter publicEndpointRateLimiter,
                           @Value("${relay.session.cookie-secure:false}") boolean secureCookie) {
        this.organizationRepository = organizationRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.passwordHasher = passwordHasher;
        this.sessionService = sessionService;
        this.auditLogService = auditLogService;
        this.publicEndpointRateLimiter = publicEndpointRateLimiter;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        if (!publicEndpointRateLimiter.trySignup(httpRequest.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many signups from this address, try again later");
        }
        if (organizationRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "an organization with this email already exists");
        }

        String rawApiKey = apiKeyHasher.generateKey();
        Organization organization = new Organization(request.organizationName(), apiKeyHasher.hash(rawApiKey));
        organization.setDashboardCredentials(request.email(), passwordHasher.hash(request.password()));
        organization = organizationRepository.save(organization);

        auditLogService.record(organization, "organization.signed_up", "email=" + request.email());
        setSessionCookie(response, sessionService.create(organization.getId()));
        return AuthResponse.from(organization, rawApiKey);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        if (!publicEndpointRateLimiter.tryLogin(httpRequest.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many login attempts from this address, try again later");
        }

        Organization organization = organizationRepository.findByEmail(request.email())
                .filter(Organization::hasDashboardCredentials)
                .filter(org -> passwordHasher.matches(request.password(), org.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));

        auditLogService.record(organization, "organization.logged_in", "email=" + request.email());
        setSessionCookie(response, sessionService.create(organization.getId()));

        // No apiKey here: only its hash is stored, and the dashboard doesn't need the raw key
        // anyway - it authenticates every subsequent request with this session cookie.
        return new AuthResponse(organization.getId(), organization.getName(), organization.getEmail(), null);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.revoke(readSessionCookie(request));
        clearSessionCookie(response);
    }

    @GetMapping("/me")
    public SessionResponse me(HttpServletRequest request) {
        Long organizationId = sessionService.resolve(readSessionCookie(request))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in"));
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in"));
        return SessionResponse.from(organization);
    }

    private String readSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (SessionService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie cookie = new Cookie(SessionService.COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge((int) sessionService.getTtl().toSeconds());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SessionService.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
