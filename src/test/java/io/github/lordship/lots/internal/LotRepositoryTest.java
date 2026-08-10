package io.github.lordship.lots.internal;

import io.github.lordship.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    private LotRow buildRow(UUID propertyId) {
        return new LotRow(
                propertyId,
                "12",
                "Rental lot",
                350.0,
                "Front row",
                1
        );
    }

    @Test
    void save_shouldPersistRow_andReturnGeneratedFields() {
        // Arrange
        UUID propertyId = insertTestProperty("I001");

        // Act
        LotRow saved = lotRepository.save(buildRow(propertyId));

        // Assert
        assertNotNull(saved.uuid());
        assertEquals(propertyId, saved.propertyId());
        assertEquals("12", saved.lotNumber());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
    }

    @Test
    void findById_shouldReturnRow_whenExists() {
        // Arrange
        UUID propertyId = insertTestProperty("I002");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        // Act
        Optional<LotRow> found = lotRepository.findById(saved.uuid());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void update_shouldChangeMutableFields() {
        // Arrange
        UUID propertyId = insertTestProperty("I003");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        // Act
        LotRow updated = lotRepository.update(new LotRow(
                saved.uuid(),
                saved.propertyId(),
                "14",
                "Vacant lot",
                "Ready for assignment",
                3,
                350.0,
                saved.createdAt(),
                saved.deletedAt()
        ));

        // Assert
        assertEquals(saved.uuid(), updated.uuid());
        assertEquals("14", updated.lotNumber());
        assertEquals("Vacant lot", updated.description());
        assertEquals("Ready for assignment", updated.notes());
        assertEquals(350.0, updated.targetRent());
        assertEquals(3, updated.sortOrder());
    }

    @Test
    void softDelete_shouldRemoveFromFindById() {
        // Arrange
        UUID propertyId = insertTestProperty("I004");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        // Act
        lotRepository.softDelete(saved.uuid());

        // Assert
        Optional<LotRow> found = lotRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());
    }

    @Test
    void patch_shouldUpdateField_andReturnUpdatedRow() {
        // Arrange
        UUID propertyId = insertTestProperty("I005");
        LotRow saved = lotRepository.save(buildRow(propertyId));
        Map<String, Object> changes = Map.of(
                "lot_number", "14",
                "description", "Vacant lot",
                "notes", "Ready for assignment",
                "target_rent", 150.0,
                "sort_order", 3
        );

        // Act
        Optional<LotRow> patched = lotRepository.patch(saved.uuid(), changes);

        // Assert
        assertTrue(patched.isPresent());
        LotRow row = patched.get();
        assertEquals("14", row.lotNumber());
        assertEquals("Vacant lot", row.description());
        assertEquals("Ready for assignment", row.notes());
        assertEquals(150.0, row.targetRent());
        assertEquals(3, row.sortOrder());
    }

    @Test
    void patch_shouldReturnUnchangedRow_withEmptyChanges() {
        // Arrange
        UUID propertyId = insertTestProperty("I006");
        LotRow saved = lotRepository.save(buildRow(propertyId));

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
        UUID propertyId = insertTestProperty("I007");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        // Act & Assert
        // @Repository exception translation wraps the IllegalArgumentException.
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                lotRepository.patch(saved.uuid(), Map.of("property_id", UUID.randomUUID()))
        );
    }

    @Test
    void patch_shouldReturnEmpty_whenRowIsSoftDeleted() {
        // Arrange
        UUID propertyId = insertTestProperty("I008");
        LotRow saved = lotRepository.save(buildRow(propertyId));
        lotRepository.softDelete(saved.uuid());

        // Act
        Optional<LotRow> patched = lotRepository.patch(saved.uuid(), Map.of("lot_number", "gone"));

        // Assert
        assertTrue(patched.isEmpty());
    }
}
