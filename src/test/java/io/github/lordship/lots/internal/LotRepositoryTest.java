package io.github.lordship.lots.internal;

import io.github.lordship.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

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
                saved.createdAt(),
                saved.deletedAt()
        ));

        // Assert
        assertEquals(saved.uuid(), updated.uuid());
        assertEquals("14", updated.lotNumber());
        assertEquals("Vacant lot", updated.description());
        assertEquals("Ready for assignment", updated.notes());
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
}
