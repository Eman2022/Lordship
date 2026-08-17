package io.github.lordship.tenancy.internal;

import com.jayway.jsonpath.JsonPath;
import io.github.lordship.TestAuthSupport;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import io.github.lordship.IntegrationTest;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.tenancy.TenancyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Transactional
public class TenancyControllerIT extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenancyRepository tenancyRepository;

    @Autowired
    TenancyService tenancyService;

    @Autowired
    JdbcClient jdbc;


    private UUID createTestTenancy(String token, UUID lotId) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/tenancy/create")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new TenancyCreateRequest(lotId)))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.uuid"));
    }

    // REPOSITORY TESTS
    @Test
    void findATenancyById() {
        TenancyRow saved = testData.insertChainToTenancy();

        Optional<TenancyRow> found = tenancyRepository.findById(saved.uuid());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void updatedAtChangesOnUpdate() {
        TenancyRow saved = testData.insertChainToTenancy();

        OffsetDateTime before = saved.updatedAt();

        TenancyRow closed = tenancyRepository.close(saved.uuid(), LocalDate.now());

        assertTrue(closed.updatedAt().isAfter(before) || closed.updatedAt().isEqual(before));
    }

    @Test
    void closingTenancy() {
        TenancyRow saved = testData.insertChainToTenancy();

        LocalDate endDate = LocalDate.now();
        TenancyRow closed = tenancyRepository.close(saved.uuid(), endDate);

        assertEquals(endDate, closed.endDate());
        assertNotNull(closed.updatedAt());
    }

    @Test
    void softDeleteRemovesFromTable() {
        TenancyRow saved = testData.insertChainToTenancy();

        tenancyRepository.softDelete(saved.uuid());

        assertTrue(tenancyRepository.findById(saved.uuid()).isEmpty());
        assertTrue(tenancyRepository.findActiveByLot(saved.lotId()).isEmpty());
    }

    // CONTROLLER TESTS
    @Test
    void getTenancy_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/tenancy/{uuid}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTenancy_shouldReturn403_whenNoTokenProvided() throws Exception {
        var request = new TenancyCreateRequest(UUID.randomUUID());

        mockMvc.perform(post("/tenancy/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTenancy_shouldReturn400_whenLotIdIsMissing() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        var invalidJson = """
                    { "lotId": null }
                """;

        mockMvc.perform(post("/tenancy/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }


    @Test
    void getActiveTenanciesByLot_shouldReturnOnlyActiveTenancies() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID lotId = testData.insertLot(testData.insertProperty("TP").uuid(), "1").uuid();

        // Create a closed tenancy
        UUID closedTenancy = createTestTenancy(token, lotId);

        // Close tenancy before creating a second
        mockMvc.perform(patch("/tenancy/{uuid}", closedTenancy)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endDate\": \"" + LocalDate.now() + "\"}"))
                .andExpect(status().isOk());

        // Create the active tenancy
        UUID activeTenancy = createTestTenancy(token, lotId);

        mockMvc.perform(get("/tenancy/lot/{lotId}", lotId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].endDate").doesNotExist());
    }

    @Test
    void patchTenancy_shouldReturn400_whenInvalidDateProvided() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID lotId = testData.insertLot(testData.insertProperty("TP").uuid(), "1").uuid();

        UUID tenancyId = createTestTenancy(token, lotId);

        // Invalid endDate
        mockMvc.perform(patch("/tenancy/{uuid}", tenancyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endDate\": \"not-a-date\"}"))
                .andExpect(status().isBadRequest());

        // Invalid startDate
        mockMvc.perform(patch("/tenancy/{uuid}", tenancyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\": \"not-a-date\"}"))
                .andExpect(status().isBadRequest());

        // Both invalid
        mockMvc.perform(patch("/tenancy/{uuid}", tenancyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "startDate": "not-a-date",
                            "endDate": "also-not-a-date"
                        }
                    """))
                .andExpect(status().isBadRequest());
    }
}