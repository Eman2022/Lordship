package io.github.lordship.tenancy.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lordship.properties.internal.PropertyRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional //todo: tests for the service and controller files
public class TenancyControllerIT {
/*
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;
*/
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

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {

        TenancyRow saved = tenancyRepository.save(buildRow(setupFullChain()));

        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNotNull(saved.endDate());
        assertNull(saved.deletedAt());
    }

    @Test
    void findATenancyById() {
        TenancyRow saved = tenancyRepository.save(buildRow(setupFullChain()));

        Optional<TenancyRow> found = tenancyRepository.findById((saved.uuid()));

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void updatedAtChangesOnUpdate() {
        TenancyRow saved = tenancyRepository.save(buildRow(setupFullChain()));

        LocalDateTime before = saved.updatedAt();

        // Close tenancyId (triggers update)
        TenancyRow closed = tenancyRepository.close(saved.uuid(), LocalDate.now());

        // Checks for difference in timestamps
        assertTrue(
                closed.updatedAt().isAfter(before)
                        || closed.updatedAt().isEqual(before)
        );
    }

    @Test
    void filterTenancyByLot() {
        // Currently can only have one tenancyId to a lot
        UUID lot = setupFullChain();
        UUID lot2 = setupFullChain();
        UUID lot3 = setupFullChain();

        tenancyRepository.save(TenancyRow.forInsert(lot, LocalDate.now(), null));
        tenancyRepository.save(TenancyRow.forInsert(lot2, LocalDate.now(), null));
        tenancyRepository.save(TenancyRow.forInsert(lot3, LocalDate.now(), LocalDate.now()));

        List<TenancyRow> active = tenancyRepository.findActiveByLot(lot);

        // Only one lot should show
        assertEquals(1, active.size());
        assertTrue(active.stream().allMatch(t -> t.endDate() == null));

        assertNotEquals(3, active.size());
        assertTrue(active.stream().allMatch(t -> t.endDate() == null));

    }

    @Test
    void closingTenancy() {
        TenancyRow saved = tenancyRepository.save(buildRow(setupFullChain()));

        LocalDate endDate = LocalDate.now();
        TenancyRow closed = tenancyRepository.close(saved.uuid(), endDate);

        assertEquals(endDate, closed.endDate());
        assertNotNull(closed.updatedAt());
    }

    @Test
    void softDeleteRemovesFromTable() {
        TenancyRow saved = tenancyRepository.save(buildRow(setupFullChain()));

        tenancyRepository.softDelete(saved.uuid());

        Optional<TenancyRow> found = tenancyRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());

        List<TenancyRow> active = tenancyRepository.findActiveByLot(saved.lotId());
        assertTrue(active.isEmpty());
    }
/*
    @Test
    void createTenancy() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        var request = new TenancyCreateRequest(lotId, accountId);

        mockMvc.perform(
                        post("/tenancies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.lotId").value(lotId.toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.startDate").exists());

        // Verify persistence
        List<TenancyRow> rows = tenancyRepository.findActiveByLot(lotId);
        assertEquals(1, rows.size());
    }
    }


    void createTenancyFailsValidation() throws Exception {
        var invalidRequest = """
        { "lotId": null, "personId": null }
    """;

        mockMvc.perform(
                        post("/tenancies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getTenancyByIdValidation() throws Exception {
        TenancyRow saved = tenancyRepository.save(
                TenancyRow.forInsert(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), null)
        );

        mockMvc.perform(get("/tenancies/" + saved.uuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(saved.uuid().toString()))
                .andExpect(jsonPath("$.lotId").value(saved.lotID().toString()));
    }
*/
}