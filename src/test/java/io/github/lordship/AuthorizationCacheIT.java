package io.github.lordship;

import io.github.lordship.TestAuthSupport.TestAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * That the cache is actually in the request path, and that an administrative edit
 * still lands on the very next request rather than a TTL later.
 *
 * <p>The per-agent eviction paths are already pinned elsewhere and deliberately not
 * repeated here: AuthBoundaryIT covers the token revoke through
 * AgentService.setAgentPassword, and RoleDeletionIT covers a revoked grant through
 * GrantedRoleService.revokeGrant, which every revoke funnels into. Both of those
 * would pass on a stale snapshot only by accident, and fail if eviction were
 * deferred to commit -- they are @Transactional and never commit.
 */
@Transactional
public class AuthorizationCacheIT extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String rootToken() throws Exception {
        return TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
    }

    private void getTenant(String token, ResultMatcher expected) throws Exception {
        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(expected);
    }

    /**
     * The whole point of the change. The first request loads the snapshot, the second
     * reads it out of memory instead of making three more round trips.
     *
     * <p>Counted after the fixture is built, so only these two requests are in scope.
     */
    @Test
    void aSecondRequestWithTheSameToken_isServedFromMemory() throws Exception {
        // Arrange
        TestAgent agent = TestAuthSupport.agentWithPermissions(
                mockMvc, objectMapper, rootToken(), "tenants:view");

        long missesBefore = authorizationCache.stats().missCount();
        long hitsBefore = authorizationCache.stats().hitCount();

        // Act
        getTenant(agent.token(), status().isNotFound());
        getTenant(agent.token(), status().isNotFound());

        // Assert
        assertEquals(missesBefore + 1, authorizationCache.stats().missCount(),
                "the first request should load the snapshot exactly once");
        assertEquals(hitsBefore + 1, authorizationCache.stats().hitCount(),
                "the second request should not have gone back to the database");
    }

    /**
     * The fan-out case: a permission added to a role has to reach every agent holding
     * it. RolePermissionService clears the whole cache rather than hunting for them,
     * and this is what says that works.
     *
     * <p>Note the shape -- the agent's snapshot is already warm from the first
     * request, so a missing eviction here would show up as a 403 rather than a 404.
     */
    @Test
    void appendingAPermissionToAHeldRole_takesEffectOnTheNextRequest() throws Exception {
        // Arrange
        String rootToken = rootToken();
        TestAgent agent = TestAuthSupport.agentWithPermissions(
                mockMvc, objectMapper, rootToken, "agent_roles:view");

        getTenant(agent.token(), status().isForbidden());

        // Act
        mockMvc.perform(post("/api/role-permissions/append")
                        .header("Authorization", "Bearer " + rootToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roleId": "%s", "permissionName": "tenants:view" }
                                """.formatted(agent.roleId())))
                .andExpect(status().isCreated());

        // Assert
        getTenant(agent.token(), status().isNotFound());
    }

    // The mirror image, so the test above cannot pass by the cache simply never
    // holding anything: taking the permission away lands on the next request too.
    @Test
    void revokingAPermissionFromAHeldRole_takesEffectOnTheNextRequest() throws Exception {
        // Arrange
        String rootToken = rootToken();
        TestAgent agent = TestAuthSupport.agentWithPermissions(
                mockMvc, objectMapper, rootToken, "tenants:view");

        getTenant(agent.token(), status().isNotFound());

        // Act
        mockMvc.perform(post("/api/role-permissions/revoke")
                        .header("Authorization", "Bearer " + rootToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roleId": "%s", "permissionName": "tenants:view" }
                                """.formatted(agent.roleId())))
                .andExpect(status().isNoContent());

        // Assert
        getTenant(agent.token(), status().isForbidden());
    }
}