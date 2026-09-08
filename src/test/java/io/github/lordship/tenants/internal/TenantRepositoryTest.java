package io.github.lordship.tenants.internal;

import io.github.lordship.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class TenantRepositoryTest extends IntegrationTest {

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    JdbcClient jdbc;

    private UUID tenancy(String propertyCode) {
        return testData.insertTenancy(
                testData.insertLot(testData.insertProperty(propertyCode).uuid(), "1").uuid()
        ).uuid();
    }

    private UUID person(String name) {
        return testData.insertPerson(name).uuid();
    }

    private Set<UUID> uuids(List<TenantRow> rows) {
        return rows.stream().map(TenantRow::uuid).collect(Collectors.toSet());
    }

    private int endTenant(UUID uuid, LocalDate endDate) {
        return jdbc.sql("UPDATE tenant SET end_date = :endDate WHERE uuid = :uuid")
                .param("endDate", endDate)
                .param("uuid", uuid)
                .update();
    }

    // ---- save ---------------------------------------------------------------

    @Test
    void save_shouldPersistTheMinimumRow_andApplyDefaults() {
        UUID tenancyId = tenancy("T101");
        UUID personId = person("Jim Halpert");
        LocalDate start = LocalDate.of(2026, 10, 1);

        TenantRow row = tenantRepository.save(tenancyId, personId, start);

        assertNotNull(row.uuid());
        assertEquals(tenancyId, row.tenancyId());
        assertEquals(personId, row.personId());
        assertEquals(start, row.startDate());
        assertNull(row.endDate());
        assertNull(row.deletedAt());
        assertNotNull(row.createdAt());
    }

    @Test
    void save_shouldAcceptANullStartDate() {
        UUID tenancyId = tenancy("T102");

        TenantRow row = tenantRepository.save(tenancyId, person("Pam Beesly"), null);

        assertNull(row.startDate());
    }

    // The whole point of the table: several people on one tenancy at once.
    @Test
    void save_shouldAllowSeveralPeopleOnOneTenancy() {
        UUID tenancyId = tenancy("T103");

        TenantRow jim = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));
        TenantRow pam = tenantRepository.save(tenancyId, person("Pam Beesly"), LocalDate.of(2026, 3, 1));

        List<TenantRow> active = tenantRepository.findActiveByTenancy(tenancyId);

        assertEquals(2, active.size());
        assertEquals(Set.of(jim.uuid(), pam.uuid()), uuids(active));
        assertNull(active.get(0).endDate());
        assertNull(active.get(1).endDate());
    }

    // ---- uq_tenant_active_person -------------------------------------------

    @Test
    void save_shouldRefuseTheSamePersonTwiceWhileBothRowsAreActive() {
        UUID tenancyId = tenancy("T104");
        UUID personId = person("Jim Halpert");

        tenantRepository.save(tenancyId, personId, LocalDate.of(2026, 1, 1));

        assertThrows(DataIntegrityViolationException.class,
                () -> tenantRepository.save(tenancyId, personId, LocalDate.of(2026, 5, 1)));
    }

    @Test
    void save_shouldAllowThatPersonBack_onceTheEarlierRowHasEnded() {
        UUID tenancyId = tenancy("T105");
        UUID personId = person("Jim Halpert");

        TenantRow first = tenantRepository.save(tenancyId, personId, LocalDate.of(2024, 1, 1));
        endTenant(first.uuid(), LocalDate.of(2025, 6, 30));

        TenantRow second = tenantRepository.save(tenancyId, personId, LocalDate.of(2026, 1, 1));

        assertNotEquals(first.uuid(), second.uuid());
        assertEquals(1, tenantRepository.findActiveByTenancy(tenancyId).size());
        assertEquals(2, tenantRepository.findByTenancy(tenancyId).size());
    }

    @Test
    void save_shouldAllowTheSamePersonOnTwoDifferentTenancies() {
        UUID personId = person("Jim Halpert");

        tenantRepository.save(tenancy("T106"), personId, LocalDate.of(2026, 1, 1));
        tenantRepository.save(tenancy("T107"), personId, LocalDate.of(2026, 1, 1));

        assertEquals(2, tenantRepository.findByPerson(personId).size());
    }

    // ---- reads --------------------------------------------------------------

    @Test
    void findById_shouldHideSoftDeletedRows() {
        UUID tenancyId = tenancy("T108");
        TenantRow row = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));

        assertTrue(tenantRepository.findById(row.uuid()).isPresent());
        assertTrue(tenantRepository.softDelete(row.uuid()));
        assertTrue(tenantRepository.findById(row.uuid()).isEmpty());
    }

    @Test
    void findByTenancy_shouldHideSoftDeletedRows() {
        UUID tenancyId = tenancy("T109");
        TenantRow kept = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));
        TenantRow removed = tenantRepository.save(tenancyId, person("Pam Beesly"), LocalDate.of(2026, 1, 1));

        tenantRepository.softDelete(removed.uuid());

        assertEquals(Set.of(kept.uuid()), uuids(tenantRepository.findByTenancy(tenancyId)));
    }

    @Test
    void findActiveByTenancy_shouldExcludeEndedRows() {
        UUID tenancyId = tenancy("T110");
        TenantRow stayed = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));
        TenantRow left = tenantRepository.save(tenancyId, person("Pam Beesly"), LocalDate.of(2026, 1, 1));

        endTenant(left.uuid(), LocalDate.of(2026, 6, 30));

        assertEquals(Set.of(stayed.uuid()), uuids(tenantRepository.findActiveByTenancy(tenancyId)));
        assertEquals(2, tenantRepository.findByTenancy(tenancyId).size());
    }

    @Test
    void findActiveByTenancyAndPerson_shouldSeeOnlyTheLiveRow() {
        UUID tenancyId = tenancy("T111");
        UUID personId = person("Jim Halpert");

        TenantRow first = tenantRepository.save(tenancyId, personId, LocalDate.of(2024, 1, 1));
        assertEquals(first.uuid(), tenantRepository.findActiveByTenancyAndPerson(tenancyId, personId)
                .orElseThrow().uuid());

        endTenant(first.uuid(), LocalDate.of(2025, 6, 30));
        assertTrue(tenantRepository.findActiveByTenancyAndPerson(tenancyId, personId).isEmpty());
    }

    // ---- patch --------------------------------------------------------------

    @Test
    void patch_shouldSetEndDate() {
        UUID tenancyId = tenancy("T112");
        TenantRow row = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));

        Optional<TenantRow> patched = tenantRepository.patch(
                row.uuid(), Map.of("end_date", LocalDate.of(2026, 6, 30)));

        assertTrue(patched.isPresent());
        assertEquals(LocalDate.of(2026, 6, 30), patched.get().endDate());
        assertEquals(LocalDate.of(2026, 1, 1), patched.get().startDate());
    }

    @Test
    void patch_shouldRejectAColumnOutsideTheAllowedSet() {
        UUID tenancyId = tenancy("T113");
        TenantRow row = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));

        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> tenantRepository.patch(row.uuid(), Map.of("person_id", UUID.randomUUID())));
    }

    @Test
    void patch_shouldReturnEmpty_forASoftDeletedRow() {
        UUID tenancyId = tenancy("T114");
        TenantRow row = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));
        tenantRepository.softDelete(row.uuid());

        assertTrue(tenantRepository.patch(row.uuid(), Map.of("end_date", LocalDate.of(2026, 6, 30))).isEmpty());
    }

    // ---- softDelete ---------------------------------------------------------

    @Test
    void softDelete_shouldReturnFalse_onASecondCall() {
        UUID tenancyId = tenancy("T115");
        TenantRow row = tenantRepository.save(tenancyId, person("Jim Halpert"), LocalDate.of(2026, 1, 1));

        assertTrue(tenantRepository.softDelete(row.uuid()));
        assertFalse(tenantRepository.softDelete(row.uuid()));
    }


}