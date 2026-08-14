package io.github.lordship.lots.internal;

import io.github.lordship.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class LotRepositoryTest extends IntegrationTest {

    @Autowired
    LotRepository lotRepository;

    @Autowired
    JdbcClient jdbc;

    private UUID insertTestProperty(String propertyCode) {
        return jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES (:propertyCode, 'Test Mobile Park', '999 Test Ave')
                RETURNING uuid
                """)
                .param("propertyCode", propertyCode)
                .query(UUID.class)
                .single();
    }

    private LotRow minimalRow(UUID propertyId, String lotNumber) {
        return new LotRow(propertyId, lotNumber);
    }

    private LotRow richRow(UUID propertyId, String lotNumber) {
        return new LotRow(
                null, propertyId, true, null,
                lotNumber, "123 Main St", "Rental lot", "Front row", 1,
                null, null, null
        );
    }

    @Test
    void save_shouldPersistMinimalRow_andApplyDefaults() {
        // Arrange
        UUID propertyId = insertTestProperty("I001");

        // Act
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Assert
        assertNotNull(saved.uuid());
        assertEquals(propertyId, saved.propertyId());
        assertEquals("12", saved.lotNumber());
        assertTrue(saved.isRentable());
        assertNull(saved.notRentableReason());
        assertNull(saved.lotAddress());
        assertNull(saved.description());
        assertNull(saved.notes());
        assertNull(saved.sortOrder());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());

        // shape_data wasn't supplied, so the DB's default rectangle applies.
        assertNotNull(saved.shapeData());
        assertEquals(4, saved.shapeData().vertices().size());
    }

    @Test
    void save_shouldPersistProvidedFields() {
        // Arrange
        UUID propertyId = insertTestProperty("I002");

        // Act
        LotRow saved = lotRepository.save(richRow(propertyId, "12"));

        // Assert
        assertEquals("123 Main St", saved.lotAddress());
        assertEquals("Rental lot", saved.description());
        assertEquals("Front row", saved.notes());
        assertEquals(1, saved.sortOrder());
        assertTrue(saved.isRentable());
    }

    @Test
    void save_shouldPersistNotRentableLot_withReason() {
        // Arrange
        UUID propertyId = insertTestProperty("I003");
        LotRow row = new LotRow(
                null, propertyId, false, "Under construction",
                "13", null, null, null, null,
                null, null, null
        );

        // Act
        LotRow saved = lotRepository.save(row);

        // Assert
        assertFalse(saved.isRentable());
        assertEquals("Under construction", saved.notRentableReason());
    }

    @Test
    void findById_shouldReturnRow_whenExists() {
        // Arrange
        UUID propertyId = insertTestProperty("I004");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Act
        Optional<LotRow> found = lotRepository.findById(saved.uuid());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void softDelete_shouldRemoveFromFindById() {
        // Arrange
        UUID propertyId = insertTestProperty("I005");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Act
        lotRepository.softDelete(saved.uuid());

        // Assert
        Optional<LotRow> found = lotRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());
    }

    @Test
    void patch_shouldUpdateFields_andReturnUpdatedRow() {
        // Arrange
        UUID propertyId = insertTestProperty("I006");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));
        Map<String, Object> changes = Map.of(
                "lot_number", "14",
                "lot_address", "456 Side St",
                "description", "Vacant lot",
                "notes", "Ready for assignment",
                "sort_order", 3,
                "is_rentable", false,
                "not_rentable_reason", "Needs inspection"
        );

        // Act
        Optional<LotRow> patched = lotRepository.patch(saved.uuid(), changes);

        // Assert
        assertTrue(patched.isPresent());
        LotRow row = patched.get();
        assertEquals("14", row.lotNumber());
        assertEquals("456 Side St", row.lotAddress());
        assertEquals("Vacant lot", row.description());
        assertEquals("Ready for assignment", row.notes());
        assertEquals(3, row.sortOrder());
        assertFalse(row.isRentable());
        assertEquals("Needs inspection", row.notRentableReason());
    }

    @Test
    void patch_shouldFail_whenSettingNotRentable_withoutReason() {
        // Arrange
        UUID propertyId = insertTestProperty("I007");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Act & Assert: the lot_not_rentable_has_reason CHECK constraint rejects this
        // at the DB level -- the same protection LotRow's compact constructor gives on
        // the Java side, verified here end-to-end through the dynamic patch path.
        assertThrows(DataIntegrityViolationException.class, () ->
                lotRepository.patch(saved.uuid(), Map.of("is_rentable", false))
        );
    }

    @Test
    void patch_shouldReturnUnchangedRow_withEmptyChanges() {
        // Arrange
        UUID propertyId = insertTestProperty("I008");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Act
        Optional<LotRow> patched = lotRepository.patch(saved.uuid(), Map.of());

        // Assert
        assertTrue(patched.isPresent());
        assertEquals(saved.uuid(), patched.get().uuid());
        assertEquals(saved.lotNumber(), patched.get().lotNumber());
    }

    @Test
    void patch_shouldThrow_whenColumnIsNotAllowed() {
        // Arrange
        UUID propertyId = insertTestProperty("I009");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Act & Assert
        // @Repository exception translation wraps the IllegalArgumentException.
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                lotRepository.patch(saved.uuid(), Map.of("property_id", UUID.randomUUID()))
        );
    }

    @Test
    void patch_shouldThrow_whenTargetRentColumnRequested() {
        // Arrange: target_rent moved to lot_permissible_agreement_type, so it's not a
        // lot column at all anymore -- it must stay off the whitelist even if some
        // caller still sends it out of habit.
        UUID propertyId = insertTestProperty("I010");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));

        // Act & Assert
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                lotRepository.patch(saved.uuid(), Map.of("target_rent", 150.0))
        );
    }

    @Test
    void patch_shouldReturnEmpty_whenRowIsSoftDeleted() {
        // Arrange
        UUID propertyId = insertTestProperty("I011");
        LotRow saved = lotRepository.save(minimalRow(propertyId, "12"));
        lotRepository.softDelete(saved.uuid());

        // Act
        Optional<LotRow> patched = lotRepository.patch(saved.uuid(), Map.of("lot_number", "gone"));

        // Assert
        assertTrue(patched.isEmpty());
    }
}