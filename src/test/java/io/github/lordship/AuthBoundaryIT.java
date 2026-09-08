package io.github.lordship;

import io.github.lordship.TestAuthSupport.TestAgent;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The one place that pins 401 against 403.
 *
 * <p>Every other IT in the suite tests the no-token path only, so nothing else
 * would notice if the two collapsed into one status. If the entry point were
 * removed, or the filter started admitting a bad token as some anonymous-ish
 * principal, every one of those tests would still pass while the distinction
 * quietly disappeared. These five do not.
 *
 * <p>The endpoints here are incidental -- /api/tenants is a stable pair of
 * view/edit authorities and nothing more. The subject is SecurityConfig and
 * JwtAuthFilter.
 *
 * <p>Deliberately not covered: the second kind of 403, where an agent holds the
 * authority but the property is outside its scope. That lives in the service
 * layer, its placement is not settled, and the status it should return has not
 * been decided -- there is nothing yet to assert.
 */
@Transactional
public class AuthBoundaryIT extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String rootToken() throws Exception {
        return TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
    }

    /** A properly signed token that expired an hour ago. */
    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        long anHourAgo = System.currentTimeMillis() - 3_600_000L;

        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("user_type", "AGENT")
                .claim("person_uuid", UUID.randomUUID().toString())
                .issuedAt(new Date(anHourAgo - 1000))
                .expiration(new Date(anHourAgo))
                .signWith(key)
                .compact();
    }

    // ---- 401: no usable identity -------------------------------------------

    @Test
    void anonymousRequest_isRefusedWith401() throws Exception {
        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unparseableToken_isRefusedWith401() throws Exception {
        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        Matchers.containsString("invalid_token")));
    }

    // The case that started this: a correctly signed token that has simply run
    // out. It used to arrive as a 403 and read as a missing permission.
    @Test
    void expiredToken_isRefusedWith401_andSaysSo() throws Exception {
        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        Matchers.containsString("expired")));
    }

    // ---- 403: authenticated, but not allowed -------------------------------

    @Test
    void authenticatedAgentWithoutTheAuthority_isRefusedWith403() throws Exception {
        TestAgent agent = TestAuthSupport.agentWithNoPermissions(mockMvc, objectMapper, rootToken());

        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isForbidden());
    }

    /**
     * The other half of the test above, and the reason it means anything: the
     * same request, same fixture machinery, with the one authority added, gets
     * past the gate and reaches the handler -- which answers 404 for a uuid that
     * does not exist. Without this, a broken token or a broken fixture would
     * produce the 403 above and look like a pass.
     */
    @Test
    void authenticatedAgentWithTheAuthority_reachesTheHandler() throws Exception {
        TestAgent agent = TestAuthSupport.agentWithPermissions(
                mockMvc, objectMapper, rootToken(), "tenants:view");

        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isNotFound());
    }

    // Authorities are per operation, not per module: reading does not imply
    // writing, and the gate is asked separately on each endpoint.
    @Test
    void viewAuthority_doesNotCarryEditAuthority() throws Exception {
        TestAgent agent = TestAuthSupport.agentWithPermissions(
                mockMvc, objectMapper, rootToken(), "tenants:view");

        mockMvc.perform(patch("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + agent.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "endDate": "2026-06-30" }
                                """))
                .andExpect(status().isForbidden());
    }

    // ---- revocation --------------------------------------------------------

    // A password change ends the sessions opened under the old one, and does not
    // touch the one opened after it. The sleep crosses a second boundary: iat is
    // whole seconds, so a token minted in the same second as the stamp is
    // deliberately left alone.
    @Test
    void changingThePassword_endsTheSessionsOpenedBeforeIt() throws Exception {
        String rootToken = rootToken();
        TestAgent agent = TestAuthSupport.agentWithPermissions(
                mockMvc, objectMapper, rootToken, "tenants:view");

        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isNotFound());

        Thread.sleep(1100);

        String newPassword = "a-different-password";
        mockMvc.perform(put("/api/agents/{uuid}/password", agent.uuid())
                        .header("Authorization", "Bearer " + rootToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newPassword": "%s" }
                                """.formatted(newPassword)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isUnauthorized());

        String freshToken = TestAuthSupport.loginAsAgent(
                mockMvc, objectMapper, agent.workEmail(), newPassword);

        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isNotFound());
    }
}