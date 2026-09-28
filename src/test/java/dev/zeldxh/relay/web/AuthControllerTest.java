package dev.zeldxh.relay.web;

import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.security.PasswordHasher;
import dev.zeldxh.relay.support.OrganizationFixtures;
import dev.zeldxh.relay.support.UniqueRemoteAddr;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    @Autowired
    private PasswordHasher passwordHasher;

    private String uniqueEmail() {
        return "owner-" + System.nanoTime() + "@example.com";
    }

    @Test
    void signupCreatesOrganizationReturnsApiKeyAndSetsSessionCookie() throws Exception {
        String email = uniqueEmail();
        MvcResult result = mockMvc.perform(post("/auth/signup").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationName\":\"Acme\",\"email\":\"" + email + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationName").value("Acme"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("relay_session");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        String body = "{\"organizationName\":\"Acme\",\"email\":\"" + email + "\",\"password\":\"correct-horse\"}";

        mockMvc.perform(post("/auth/signup").with(UniqueRemoteAddr.unique()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/signup").with(UniqueRemoteAddr.unique()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void signupRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/auth/signup").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationName\":\"Acme\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithCorrectPasswordSetsSessionCookieUsableForProtectedEndpoints() throws Exception {
        String email = uniqueEmail();
        OrganizationFixtures.seedWithCredentials(organizationRepository, apiKeyHasher, passwordHasher, "Acme", email, "correct-horse");

        MvcResult login = mockMvc.perform(post("/auth/login").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andReturn();

        Cookie sessionCookie = login.getResponse().getCookie("relay_session");
        assertThat(sessionCookie).isNotNull();

        mockMvc.perform(get("/endpoints").cookie(sessionCookie))
                .andExpect(status().isOk());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String email = uniqueEmail();
        OrganizationFixtures.seedWithCredentials(organizationRepository, apiKeyHasher, passwordHasher, "Acme", email, "correct-horse");

        mockMvc.perform(post("/auth/login").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsUnknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentOrganizationWhenSessionCookiePresent() throws Exception {
        String email = uniqueEmail();
        OrganizationFixtures.seedWithCredentials(organizationRepository, apiKeyHasher, passwordHasher, "Acme", email, "correct-horse");
        MvcResult login = mockMvc.perform(post("/auth/login").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse\"}"))
                .andReturn();
        Cookie sessionCookie = login.getResponse().getCookie("relay_session");

        mockMvc.perform(get("/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void meReturnsUnauthorizedWithoutSessionCookie() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesSessionSoSubsequentMeCallFails() throws Exception {
        String email = uniqueEmail();
        OrganizationFixtures.seedWithCredentials(organizationRepository, apiKeyHasher, passwordHasher, "Acme", email, "correct-horse");
        MvcResult login = mockMvc.perform(post("/auth/login").with(UniqueRemoteAddr.unique())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse\"}"))
                .andReturn();
        Cookie sessionCookie = login.getResponse().getCookie("relay_session");

        mockMvc.perform(post("/auth/logout").cookie(sessionCookie)).andExpect(status().isNoContent());
        mockMvc.perform(get("/auth/me").cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitsRepeatedLoginAttemptsFromTheSameAddress() throws Exception {
        String email = uniqueEmail();
        OrganizationFixtures.seedWithCredentials(organizationRepository, apiKeyHasher, passwordHasher, "Acme", email, "correct-horse");
        var sameAddress = UniqueRemoteAddr.unique();

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/auth/login")
                            .with(sameAddress)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .with(sameAddress)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void apiKeyStillWorksForOrganizationsWithoutDashboardCredentials() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "api-only-org");

        mockMvc.perform(get("/endpoints").header("Authorization", org.authorizationHeader()))
                .andExpect(status().isOk());
    }
}
