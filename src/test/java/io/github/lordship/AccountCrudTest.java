package io.github.lordship;

import io.github.lordship.access.AgentLoginRequest;
import io.github.lordship.accounts.AccountService;
import io.github.lordship.accounts.AccountStatus;
import io.github.lordship.accounts.internal.AccountCreationRequest;
import io.github.lordship.accounts.internal.AccountUpdateRequest;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.Lot;
import io.github.lordship.lots.LotCreationRequest;
import io.github.lordship.lots.LotService;
import io.github.lordship.properties.Property;
import io.github.lordship.properties.PropertyService;
import io.github.lordship.tenancy.TenancyService;
import io.github.lordship.tenancy.internal.TenancyCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

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
    PropertyService propertyService;

    @Autowired
    LotService lotService;

    @Autowired
    TenancyService tenancyService;

    @Autowired
    AccountService accountService;

    @MockitoBean
    AuditService auditService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String loginAsRoot() throws Exception {
        AgentLoginRequest loginRequest = new AgentLoginRequest(rootEmail, rootPassword);

        MvcResult result = mockMvc.perform(post("/api/agents/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    private UUID setupFullChain() {
        Property property = propertyService.createProperty("Test Mobile Park", "999 Test Ave");
        Lot lot = lotService.createLot(new LotCreationRequest(property.uuid(), "1", null, null, null, null));
        return tenancyService.create(new TenancyCreateRequest(lot.uuid())).uuid();
    }

    private UUID getAutoCreatedAccountId(UUID tenancyId) {
        return accountService.getAccountByTenancyId(tenancyId).orElseThrow().uuid();
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
        AccountUpdateRequest request = new AccountUpdateRequest(AccountStatus.ACTIVE, false, null);

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
    void authorizedGetAutoCreatedAccountReturns200() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();
        UUID accountId = getAutoCreatedAccountId(tenancyId);

        mockMvc.perform(get("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(accountId.toString()))
                .andExpect(jsonPath("$.tenancyId").value(tenancyId.toString()))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.balanceCached").value(0))
                .andExpect(jsonPath("$.autopayEnabled").value(false));
    }

    @Test
    void authorizedGetByIdReturns200() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();
        String accountId = getAutoCreatedAccountId(tenancyId).toString();

        mockMvc.perform(get("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(accountId))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void authorizedUpdateReturns200() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();
        String accountId = getAutoCreatedAccountId(tenancyId).toString();

        AccountUpdateRequest updateRequest = new AccountUpdateRequest(
                AccountStatus.DELINQUENT,
                true,
                "Tenant missed payment"
        );

        mockMvc.perform(put("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("DELINQUENT"))
                .andExpect(jsonPath("$.balanceCached").value(0))
                .andExpect(jsonPath("$.autopayEnabled").value(true))
                .andExpect(jsonPath("$.notes").value("Tenant missed payment"));
    }

    @Test
    void authorizedDeleteReturns204() throws Exception {
        String token = loginAsRoot();
        UUID tenancyId = setupFullChain();
        String accountId = getAutoCreatedAccountId(tenancyId).toString();

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
