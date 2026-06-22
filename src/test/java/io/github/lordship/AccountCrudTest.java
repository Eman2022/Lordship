package io.github.lordship;

import io.github.lordship.access.AgentLoginRequest;
import io.github.lordship.accounts.AccountCreationRequest;
import io.github.lordship.accounts.AccountStatus;
import io.github.lordship.accounts.internal.AccountUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AccountCrudTest {

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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    private String insertTestProperty() {
        jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave')
                """).update();
        return "TST01";
    }

    private UUID insertTestLot(String propertyCode) {
        return jdbc.sql("""
                INSERT INTO lot (property_code, lot_number)
                VALUES (:propertyCode, '1')
                RETURNING uuid
                """)
                .param("propertyCode", propertyCode)
                .query(UUID.class)
                .single();
    }

    private UUID insertTestTenancy(UUID lotId) {
        return jdbc.sql("""
                INSERT INTO tenancy (lot_number, account_number, start_date)
                VALUES (:lotId, :placeholderAccountId, CURRENT_DATE)
                RETURNING uuid
                """)
                .param("lotId", lotId)
                .param("placeholderAccountId", UUID.randomUUID())
                .query(UUID.class)
                .single();
    }

    private UUID setupFullChain() {
        String propertyCode = insertTestProperty();
        UUID lotId = insertTestLot(propertyCode);
        return insertTestTenancy(lotId);
    }

    // -------------------------------------------------------------------------
    // Tests: unauthorized access returns 403
    // -------------------------------------------------------------------------

    @Test
    void unauthorizedCreateReturns403() throws Exception {
        AccountCreationRequest request = new AccountCreationRequest(UUID.randomUUID(), null);

        mockMvc.perform(post("/accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedGetByIdReturns403() throws Exception {
        mockMvc.perform(get("/accounts/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedGetByPropertyReturns403() throws Exception {
        mockMvc.perform(get("/accounts/property/TST01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedUpdateReturns403() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest(AccountStatus.ACTIVE, BigDecimal.ZERO, false, null);

        mockMvc.perform(put("/accounts/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedDeleteReturns403() throws Exception {
        mockMvc.perform(delete("/accounts/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Tests: authorized requests return correct responses
    // -------------------------------------------------------------------------

    @Test
    void authorizedCreateReturns201() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();

        AccountCreationRequest request = new AccountCreationRequest(tenancyId, "New tenant account");

        mockMvc.perform(post("/accounts/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.tenancyId").value(tenancyId.toString()))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.autopayEnabled").value(false));
    }

    @Test
    void authorizedGetByIdReturns200() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();

        // create account
        AccountCreationRequest createRequest = new AccountCreationRequest(tenancyId, null);
        MvcResult createResult = mockMvc.perform(post("/accounts/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("uuid").asString();

        // fetch by id
        mockMvc.perform(get("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(accountId))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void authorizedGetByPropertyReturns200WithAccounts() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();

        AccountCreationRequest createRequest = new AccountCreationRequest(tenancyId, null);
        mockMvc.perform(post("/accounts/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/property/TST01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").exists())
                .andExpect(jsonPath("$[0].accountStatus").value("ACTIVE"));
    }

    @Test
    void authorizedUpdateReturns200() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();

        AccountCreationRequest createRequest = new AccountCreationRequest(tenancyId, null);
        MvcResult createResult = mockMvc.perform(post("/accounts/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("uuid").asString();

        AccountUpdateRequest updateRequest = new AccountUpdateRequest(
                AccountStatus.DELINQUENT,
                new BigDecimal("325.00"),
                true,
                "Tenant missed payment"
        );

        mockMvc.perform(put("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("DELINQUENT"))
                .andExpect(jsonPath("$.balance").value(325.00))
                .andExpect(jsonPath("$.autopayEnabled").value(true))
                .andExpect(jsonPath("$.notes").value("Tenant missed payment"));
    }

    @Test
    void authorizedDeleteReturns204() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();

        AccountCreationRequest createRequest = new AccountCreationRequest(tenancyId, null);
        MvcResult createResult = mockMvc.perform(post("/accounts/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("uuid").asString();

        // soft delete
        mockMvc.perform(delete("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // confirm it's gone
        mockMvc.perform(get("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
