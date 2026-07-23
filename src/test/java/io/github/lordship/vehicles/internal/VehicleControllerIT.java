package io.github.lordship.vehicles.internal;

import io.github.lordship.TestAuthSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional

public class VehicleControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcClient jdbc;

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    private UUID insertTestProperty() {
        return jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave')
                RETURNING uuid
                """)
                .query(UUID.class)
                .single();
    }

    private UUID insertTestLot(UUID propertyUuid) {
        return jdbc.sql("""
                INSERT INTO lot (property_id, lot_number)
                VALUES (:propertyUuid, '1')
                RETURNING uuid
                """)
                .param("propertyUuid", propertyUuid)
                .query(UUID.class)
                .single();
    }

    private UUID insertTestTenancy(UUID lotUuid) {
        return jdbc.sql("""
                INSERT INTO tenancy (lot_id, start_date)
                VALUES (:lotUuid, CURRENT_DATE)
                RETURNING uuid
                """)
                .param("lotUuid", lotUuid)
                .query(UUID.class)
                .single();
    }

    private UUID setupFullChain() {
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        return insertTestTenancy(lotUuid);
    }

    private UUID setupFullChainAndGetProperty() {
        return insertTestProperty();
    }

    private Map<String, Object> buildVehicleRequest(UUID tenancyUuid, UUID propertyUuid) {
        return Map.of(
                "tenancyUuid",   tenancyUuid.toString(),
                "propertyUuid",  propertyUuid.toString(),
                "make",          "Toyota",
                "model",         "Camry",
                "year",          2020,
                "plateNumber",   "ABC123",
                "plateState",    "WA",
                "color",         "Blue"
        );
    }

    private String loginAsRoot() throws Exception {
        return TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
    }

    // ── Unauthorized tests ────────────────────────────────────

    @Test
    void unauthorizedRegisterReturns403() throws Exception {
        mockMvc.perform(post("/vehicles/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(UUID.randomUUID(), UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedGetByIdReturns403() throws Exception {
        mockMvc.perform(get("/vehicles/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedGetByTenancyReturns403() throws Exception {
        mockMvc.perform(get("/vehicles/tenancy/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedGetByPropertyReturns403() throws Exception {
        mockMvc.perform(get("/vehicles/property/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedPatchReturns403() throws Exception {
        mockMvc.perform(patch("/vehicles/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("color", "Red"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedDeleteReturns403() throws Exception {
        mockMvc.perform(delete("/vehicles/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedGetPolicyReturns403() throws Exception {
        mockMvc.perform(get("/vehicles/policy/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedSetPolicyReturns403() throws Exception {
        mockMvc.perform(put("/vehicles/policy/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "freeVehicleLimit", 2,
                                "extraVehicleFee",  "25.00"
                        ))))
                .andExpect(status().isForbidden());
    }

    // ── Authorized tests ──────────────────────────────────────

    @Test
    void authorizedRegisterReturns201() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        UUID tenancyUuid = insertTestTenancy(lotUuid);

        mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid, propertyUuid))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicle.uuid").exists())
                .andExpect(jsonPath("$.vehicle.make").value("Toyota"))
                .andExpect(jsonPath("$.vehicle.plateNumber").value("ABC123"))
                .andExpect(jsonPath("$.plateConflictFlagged").value(false));
    }

    @Test
    void authorizedGetByIdReturns200() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        UUID tenancyUuid = insertTestTenancy(lotUuid);

        // Register first
        String registerBody = mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid, propertyUuid))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String vehicleUuid = objectMapper.readTree(registerBody)
                .get("vehicle").get("uuid").asString();

        // Fetch by ID
        mockMvc.perform(get("/vehicles/" + vehicleUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(vehicleUuid))
                .andExpect(jsonPath("$.make").value("Toyota"));
    }

    @Test
    void authorizedGetByTenancyReturns200() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        UUID tenancyUuid = insertTestTenancy(lotUuid);

        mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid, propertyUuid))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/vehicles/tenancy/" + tenancyUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].make").value("Toyota"))
                .andExpect(jsonPath("$[0].plateNumber").value("ABC123"));
    }

    @Test
    void authorizedGetByPropertyReturns200() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        UUID tenancyUuid = insertTestTenancy(lotUuid);

        mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid, propertyUuid))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/vehicles/property/" + propertyUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].make").value("Toyota"));
    }

    @Test
    void authorizedPatchReturns200() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        UUID tenancyUuid = insertTestTenancy(lotUuid);

        String registerBody = mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid, propertyUuid))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String vehicleUuid = objectMapper.readTree(registerBody)
                .get("vehicle").get("uuid").asString();

        mockMvc.perform(patch("/vehicles/" + vehicleUuid)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("color", "Red"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("Red"))
                .andExpect(jsonPath("$.make").value("Toyota"));
    }

    @Test
    void authorizedDeleteReturns204() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();
        UUID lotUuid = insertTestLot(propertyUuid);
        UUID tenancyUuid = insertTestTenancy(lotUuid);

        String registerBody = mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid, propertyUuid))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String vehicleUuid = objectMapper.readTree(registerBody)
                .get("vehicle").get("uuid").asString();

        // Delete
        mockMvc.perform(delete("/vehicles/" + vehicleUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Confirm gone
        mockMvc.perform(get("/vehicles/" + vehicleUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void authorizedSetAndGetPolicyReturns200() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();

        // Set policy
        mockMvc.perform(put("/vehicles/policy/" + propertyUuid)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "freeVehicleLimit", 2,
                                "extraVehicleFee",  "25.00",
                                "notes",            "Standard policy"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freeVehicleLimit").value(2))
                .andExpect(jsonPath("$.extraVehicleFee").value(25.00));

        // Get policy
        mockMvc.perform(get("/vehicles/policy/" + propertyUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freeVehicleLimit").value(2))
                .andExpect(jsonPath("$.propertyUuid").value(propertyUuid.toString()));
    }

    @Test
    void registerFlagsPlateConflictAcrossTenancies() throws Exception {
        String token = loginAsRoot();
        UUID propertyUuid = insertTestProperty();

        UUID lotUuid1 = insertTestLot(propertyUuid);
        UUID tenancyUuid1 = insertTestTenancy(lotUuid1);

        UUID lotUuid2 = insertTestLot(propertyUuid);
        UUID tenancyUuid2 = insertTestTenancy(lotUuid2);

        // Register plate under tenancy 1
        mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid1, propertyUuid))))
                .andExpect(status().isCreated());

        // Register same plate under tenancy 2 — should flag conflict
        mockMvc.perform(post("/vehicles/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildVehicleRequest(tenancyUuid2, propertyUuid))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateConflictFlagged").value(true))
                .andExpect(jsonPath("$.conflictingVehicles").isArray())
                .andExpect(jsonPath("$.conflictingVehicles[0].plateNumber").value("ABC123"));
    }
}
