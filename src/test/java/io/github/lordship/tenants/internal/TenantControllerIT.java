package io.github.lordship.tenants.internal;

import com.jayway.jsonpath.JsonPath;
import io.github.lordship.TestAuthSupport;
import io.github.lordship.access.AgentLoginRequest;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.tenancy.internal.TenancyCreateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import io.github.lordship.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Transactional
public class TenantControllerIT extends IntegrationTest {
    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcClient jdbc;

    private String loginAsRoot() throws Exception {
        AgentLoginRequest loginRequest = new AgentLoginRequest(rootEmail, rootPassword);

        MvcResult result = mockMvc.perform(post("/agents/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    private UUID insertTestProperty() {
        PropertyRow propertyRow = jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave') RETURNING *
                """).query(PropertyRow.class)
                .single();
        return propertyRow.uuid();
    }

    private UUID insertTestLot(UUID propertyId) {
        return jdbc.sql("""
                INSERT INTO lot (property_id, lot_number)
                VALUES (:propertyId, '1')
                RETURNING uuid
                """)
                .param("propertyId", propertyId)
                .query(UUID.class)
                .single();
    }

    private UUID setupFullChain() {
        UUID propertyId = insertTestProperty();
        return insertTestLot(propertyId);
    }


    @Test
    void createTenant_unauthorized_returns403() throws Exception {
        var request = new TenantCreateRequest(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/tenants/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "tenants:create")
    void createTenant_invalidPayload_returns400() throws Exception {
        var invalidJson = """
                    { "tenancyId": null, "personId": null }
                """;

        mockMvc.perform(
                        post("/tenants/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest());
    }



    @Test
    void getTenant_shouldReturn404_whenTenantDoesNotExist() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);

        mockMvc.perform(get("/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }



    @Test
    @WithMockUser(authorities = "tenants:update")
    void patchTenant_shouldReturn400_whenInvalidDateProvided() throws Exception {
        String invalidJson = """
        { "startDate": "not-a-date" }
    """;

        mockMvc.perform(patch("/tenants/{uuid}", setupFullChain())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTenant_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/tenants/{uuid}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
