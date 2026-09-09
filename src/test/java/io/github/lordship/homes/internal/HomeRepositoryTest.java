package io.github.lordship.homes.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.homes.HomeCondition;
import io.github.lordship.lots.internal.LotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class HomeRepositoryTest extends IntegrationTest {

    @Autowired
    HomeRepository homeRepository;

    @Autowired
    LotRepository lotRepository;

    private UUID lotOn(String propertyCode, String lotNumber) {
        UUID propertyId = testData.insertProperty(propertyCode).uuid();
        return testData.insertLot(propertyId, lotNumber).uuid();
    }

    // ── save ─────────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistMinimalRow_andApplyDefaults() {
        // Arrange
        UUID lotId = lotOn("H001", "4B");

        // Act
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        // Assert
        assertNotNull(saved.uuid());
        assertEquals(lotId, saved.lotId());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());

        // DB defaults, since neither was supplied
        assertEquals("FT", saved.dimensionsUnits());
        assertFalse(saved.parkOwned());

        // generated from the lot; section count is unknown at insert
        assertEquals("Mobile home on lot 4B", saved.name());

        // everything else waits for a PATCH, per the create-minimal design
        assertNull(saved.sections());
        assertNull(saved.condition());
        assertNull(saved.estimatedValue());
        assertNull(saved.vin());
    }

    @Test
    void save_shouldRejectACreatedByThatNamesNoAgent() {
        // created_by is a foreign key: the acting agent comes from AuditContext, so a
        // value that matches no agent means something upstream invented one
        UUID lotId = lotOn("H002", "1");

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.save(lotId, UUID.randomUUID()));
    }

    @Test
    void save_shouldReturnEmpty_whenTheLotDoesNotExist() {
        // Act & Assert: selecting from lot means a bad id inserts nothing rather than
        // tripping the foreign key
        assertTrue(homeRepository.save(UUID.randomUUID(), null).isEmpty());
    }

    @Test
    void save_shouldReturnEmpty_whenTheLotIsSoftDeleted() {
        // Arrange: the foreign key alone would accept this -- deleted_at means nothing to it
        UUID lotId = lotOn("H003", "7");
        lotRepository.softDelete(lotId);

        // Act & Assert
        assertTrue(homeRepository.save(lotId, null).isEmpty());
    }

    // ── reads ────────────────────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnRow_whenExists() {
        UUID lotId = lotOn("H004", "2");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        Optional<HomeRow> found = homeRepository.findById(saved.uuid());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void findById_doesNotFindHome_afterSoftDelete() {
        UUID lotId = lotOn("H005", "2");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertTrue(homeRepository.softDelete(saved.uuid()));

        assertTrue(homeRepository.findById(saved.uuid()).isEmpty());
    }

    @Test
    void softDelete_shouldReturnFalse_whenAlreadyDeleted() {
        // the boolean is what tells the service whether to write an audit row
        UUID lotId = lotOn("H006", "2");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertTrue(homeRepository.softDelete(saved.uuid()));
        assertFalse(homeRepository.softDelete(saved.uuid()));
    }

    @Test
    void findByLot_shouldReturnEveryActiveHomeOnTheLot() {
        // Arrange: two at once is legal -- one being pulled while the next is set
        UUID lotId = lotOn("H007", "3");
        homeRepository.save(lotId, null).orElseThrow();
        homeRepository.save(lotId, null).orElseThrow();

        // Act
        List<HomeRow> found = homeRepository.findByLot(lotId);

        // Assert
        assertEquals(2, found.size());
    }

    @Test
    void findByLot_shouldExcludeSoftDeletedHomes() {
        UUID lotId = lotOn("H008", "3");
        HomeRow keep = homeRepository.save(lotId, null).orElseThrow();
        HomeRow drop = homeRepository.save(lotId, null).orElseThrow();
        homeRepository.softDelete(drop.uuid());

        List<HomeRow> found = homeRepository.findByLot(lotId);

        assertEquals(1, found.size());
        assertEquals(keep.uuid(), found.get(0).uuid());
    }

    @Test
    void findByProperty_shouldReachHomesThroughTheirLots() {
        // Arrange
        UUID propertyId = testData.insertProperty("H009").uuid();
        UUID lotA = testData.insertLot(propertyId, "1").uuid();
        UUID lotB = testData.insertLot(propertyId, "2").uuid();
        homeRepository.save(lotA, null).orElseThrow();
        homeRepository.save(lotB, null).orElseThrow();

        // a home at a different park must not appear
        homeRepository.save(lotOn("H010", "1"), null).orElseThrow();

        // Act
        List<HomeRow> found = homeRepository.findByProperty("H009");

        // Assert
        assertEquals(2, found.size());
    }

    @Test
    void findByProperty_shouldReturnEmpty_forAnUnknownCode() {
        assertTrue(homeRepository.findByProperty("NOPE").isEmpty());
    }

    @Test
    void findByVin_shouldIgnoreCase() {
        // Arrange: inherited paperwork is not consistent about case
        UUID lotId = lotOn("H011", "5");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();
        homeRepository.patch(saved.uuid(), Map.of("vin", "flw12345ab"));

        // Act & Assert
        assertEquals(1, homeRepository.findByVin("FLW12345AB").size());
        assertEquals(1, homeRepository.findByVin("flw12345ab").size());
        assertEquals(1, homeRepository.findByVin("Flw12345Ab").size());
        assertTrue(homeRepository.findByVin("nothing").isEmpty());
    }

    @Test
    void findLotNumber_shouldFeedTheGeneratedName() {
        UUID lotId = lotOn("H012", "12C");

        assertEquals(Optional.of("12C"), homeRepository.findLotNumber(lotId));
        assertTrue(homeRepository.findLotNumber(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findLotNumber_shouldReturnEmpty_forASoftDeletedLot() {
        UUID lotId = lotOn("H013", "8");
        lotRepository.softDelete(lotId);

        assertTrue(homeRepository.findLotNumber(lotId).isEmpty());
    }

    // ── patch ────────────────────────────────────────────────────────────────────

    @Test
    void patch_shouldUpdateFields_andReturnUpdatedRow() {
        // Arrange
        UUID lotId = lotOn("H014", "4B");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        Map<String, Object> changes = Map.of(
                "make", "Fleetwood",
                "model", "Broadmore",
                "model_year", 1998,
                "sections", 2,
                "bedroom_count", 3,
                "bathroom_count", new BigDecimal("1.5"),
                "condition", "GOOD",
                "park_owned", true,
                "estimated_value", new BigDecimal("42500.75"),
                "estimated_value_on", LocalDate.of(2026, 8, 1)
        );

        // Act
        HomeRow row = homeRepository.patch(saved.uuid(), changes).orElseThrow();

        // Assert
        assertEquals("Fleetwood", row.make());
        assertEquals(1998, row.modelYear());
        assertEquals(2, row.sections());
        assertEquals(0, new BigDecimal("1.5").compareTo(row.bathroomCount()));
        assertEquals(HomeCondition.GOOD, row.condition());
        assertTrue(row.parkOwned());
        assertEquals(0, new BigDecimal("42500.75").compareTo(row.estimatedValue()));
        assertEquals(LocalDate.of(2026, 8, 1), row.estimatedValueOn());
    }

    @Test
    void patch_shouldClearAField_whenGivenNull() {
        UUID lotId = lotOn("H015", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();
        homeRepository.patch(saved.uuid(), Map.of("make", "Fleetwood"));

        Map<String, Object> changes = new HashMap<>();
        changes.put("make", null);

        assertNull(homeRepository.patch(saved.uuid(), changes).orElseThrow().make());
    }

    @Test
    void patch_shouldReturnUnchangedRow_withEmptyChanges() {
        UUID lotId = lotOn("H016", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        HomeRow patched = homeRepository.patch(saved.uuid(), Map.of()).orElseThrow();

        assertEquals(saved.uuid(), patched.uuid());
        assertEquals(saved.name(), patched.name());
    }

    @Test
    void patch_shouldReturnEmpty_whenRowIsSoftDeleted() {
        UUID lotId = lotOn("H017", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();
        homeRepository.softDelete(saved.uuid());

        assertTrue(homeRepository.patch(saved.uuid(), Map.of("make", "gone")).isEmpty());
    }

    @Test
    void patch_shouldThrow_whenColumnIsNotAllowed() {
        UUID lotId = lotOn("H018", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        // @Repository exception translation wraps the IllegalArgumentException
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("created_by", UUID.randomUUID())));
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("deleted_at", OffsetDateTime.now(ZoneOffset.UTC))));
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("uuid", UUID.randomUUID())));
    }

    // ── the CHECK constraints, reached past the service ──────────────────────────

    @Test
    void patch_shouldBeRejected_byTheConditionCheck() {
        // The service validates against HomeCondition first, so this is the backstop:
        // a caller going straight at the repository still cannot write a bad grade.
        UUID lotId = lotOn("H019", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("condition", "MEDIOCRE")));
    }

    // One constraint violation per test method, deliberately. Postgres aborts the
    // whole transaction on the first failed statement (SQLSTATE 25P02) and refuses
    // everything after it until rollback, so a second assertThrows in the same test
    // would report "current transaction is aborted" instead of its own violation.
    // Whitelist rejections above can share a method: they throw in Java before any
    // SQL is sent, so the transaction stays clean.

    @Test
    void patch_shouldBeRejected_whenSectionsIsBelowOne() {
        UUID lotId = lotOn("H020", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("sections", 0)));
    }

    @Test
    void patch_shouldBeRejected_whenSectionsIsAboveTheCeiling() {
        UUID lotId = lotOn("H021", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("sections", 9)));
    }

    @Test
    void patch_shouldBeRejected_whenEstimatedValueIsNegative() {
        UUID lotId = lotOn("H024", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("estimated_value", new BigDecimal("-1"))));
    }

    @Test
    void patch_shouldBeRejected_whenBedroomCountIsNegative() {
        UUID lotId = lotOn("H025", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("bedroom_count", -1)));
    }

    @Test
    void patch_shouldBeRejected_whenWidthIsNotPositive() {
        UUID lotId = lotOn("H026", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("width", new BigDecimal("0"))));
    }

    @Test
    void patch_shouldBeRejected_byTheDimensionsUnitsCheck() {
        UUID lotId = lotOn("H022", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("dimensions_units", "CUBITS")));
    }

    @Test
    void patch_shouldBeRejected_whenAValuationDateHasNoValue() {
        // mobile_home_valuation_needs_value: a date with nothing to date is meaningless
        UUID lotId = lotOn("H023", "1");
        HomeRow saved = homeRepository.save(lotId, null).orElseThrow();

        assertThrows(DataIntegrityViolationException.class, () ->
                homeRepository.patch(saved.uuid(), Map.of("estimated_value_on", LocalDate.of(2026, 8, 1))));
    }

    @Test
    void homeRow_shouldRejectAValuationDateWithNoValue_onTheJavaSide() {
        // the same rule as the CHECK above, so a row can never be built in a state the
        // database would refuse
        assertThrows(IllegalArgumentException.class, () -> new HomeRow(
                UUID.randomUUID(), "Blue one", null, null, LocalDate.of(2026, 8, 1),
                null, null, null, null, null, null, null, "FT", null, null,
                null, null, null, null, false, OffsetDateTime.now(ZoneOffset.UTC), null, null
        ));
    }
}
