package io.github.lordship.tenancy.internal;

import tools.jackson.databind.ObjectMapper;
import io.github.lordship.IntegrationTest;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.tenancy.TenancyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenancyRepository tenancyRepository;

    @Autowired
    JdbcClient jdbc;

    private TenancyRow buildRow(UUID lotId) {
        return TenancyRow.forInsert(
                lotId,
                LocalDate.now(),
                LocalDate.of(2026, 10, 21)
        );
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


    // REPOSITORY TESTS
    @Test
    void findATenancyById() {
        UUID lotId = setupFullChain();
        TenancyRow saved = tenancyRepository.save(buildRow(lotId));

        Optional<TenancyRow> found = tenancyRepository.findById(saved.uuid());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void updatedAtChangesOnUpdate() {
        UUID lotId = setupFullChain();
        TenancyRow saved = tenancyRepository.save(buildRow(lotId));

        OffsetDateTime before = saved.updatedAt();

        TenancyRow closed = tenancyRepository.close(saved.uuid(), LocalDate.now());

        assertTrue(closed.updatedAt().isAfter(before) || closed.updatedAt().isEqual(before));
    }

    @Test
    void closingTenancy() {
        UUID lotId = setupFullChain();
        TenancyRow saved = tenancyRepository.save(buildRow(lotId));

        LocalDate endDate = LocalDate.now();
        TenancyRow closed = tenancyRepository.close(saved.uuid(), endDate);

        assertEquals(endDate, closed.endDate());
        assertNotNull(closed.updatedAt());
    }

    @Test
    void softDeleteRemovesFromTable() {
        UUID lotId = setupFullChain();
        TenancyRow saved = tenancyRepository.save(buildRow(lotId));

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
    @WithMockUser(authorities = "tenancy:create")
    void createTenancy_shouldReturn400_whenLotIdIsMissing() throws Exception {
        var invalidJson = """
                    { "lotId": null }
                """;

        mockMvc.perform(post("/tenancy/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(authorities = "tenancy:view")
    void getActiveTenanciesByLot_shouldReturnOnlyActiveTenancies() throws Exception {
        UUID lotId = setupFullChain();

        tenancyRepository.save(TenancyRow.forInsert(lotId, LocalDate.now(), null));
        tenancyRepository.save(TenancyRow.forInsert(lotId, LocalDate.now().minusDays(10), LocalDate.now()));

        mockMvc.perform(get("/tenancy/lot/{lotId}", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].endDate").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "tenancy:edit")
    void patchTenancy_shouldReturn400_whenInvalidDateProvided() throws Exception {
        UUID lotId = setupFullChain();

        TenancyRow saved = tenancyRepository.save(
                TenancyRow.forInsert(lotId, LocalDate.now(), null)
        );

        var invalidJson = """
            { "endDate": "not-a-date" }
            """;

        mockMvc.perform(patch("/tenancy/{uuid}", saved.uuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}