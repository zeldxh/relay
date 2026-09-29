package dev.zeldxh.relay.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * relay is a JSON API with no web UI of its own - this exists purely so hitting the bare
 * root in a browser returns something useful instead of Spring's default Whitelabel error
 * page, which is what you'd otherwise get (the auth filter 401s on "/" since it isn't
 * mapped to anything, and there's no custom /error handling).
 */
@RestController
public class RootController {

    private final String docsUrl;
    private final String dashboardUrl;

    public RootController(@Value("${relay.docs-url}") String docsUrl,
                           @Value("${relay.dashboard-url}") String dashboardUrl) {
        this.docsUrl = docsUrl;
        this.dashboardUrl = dashboardUrl;
    }

    @GetMapping("/")
    public RootResponse root() {
        return new RootResponse(
                "relay",
                "Multi-tenant webhook delivery service. This is the API - see the links below for the human-facing docs and dashboard.",
                docsUrl,
                dashboardUrl,
                "/actuator/health"
        );
    }

    public record RootResponse(String name, String description, String docs, String dashboard, String health) {
    }
}
