package dev.zeldxh.relay.web;

import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.support.OrganizationFixtures;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private Long createEndpoint(String authHeader, String path) throws Exception {
        MvcResult result = mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders\",\"url\":\"http://localhost:" + port + path + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void marksEndpointVerifiedWhenItRespondsSuccessfully() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        Long endpointId = createEndpoint(org.authorizationHeader(), "/hook");

        mockMvc.perform(post("/endpoints/" + endpointId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verifiedAt").exists());
    }

    @Test
    void doesNotVerifyWhenEndpointRespondsWithError() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        Long endpointId = createEndpoint(org.authorizationHeader(), "/hook");

        mockMvc.perform(post("/endpoints/" + endpointId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void doesNotVerifyWhenEndpointIsUnreachable() throws Exception {
        // server deliberately never started
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        Long endpointId = createEndpoint(org.authorizationHeader(), "/hook");

        mockMvc.perform(post("/endpoints/" + endpointId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader()))
                .andExpect(status().isUnprocessableEntity());
    }
}
