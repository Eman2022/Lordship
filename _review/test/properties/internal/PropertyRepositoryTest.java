package io.github.lordship.properties.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional

public class PropertyRepositoryTest {
    @Autowired
    PropertyRepository propertyRepository;


    @Test
    void save_persistsRow_andReturnsGeneratedFields() {
        // Arrange and Act
        PropertyRow saved = propertyRepository.save("Test Mobile Park", "999 Test Ave", "TP");

        // Assert
        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        assertEquals("Test Mobile Park", saved.propertyName());
        assertEquals("999 Test Ave", saved.propertyAddress());
    }

    @Test
    void findByPropertyCodeReturnsSavedProperty() {
        // Arrange
        PropertyRow saved = propertyRepository.save("Test Mobile Park", "999 Test Ave", "TP");

        // Act
        Optional<PropertyRow> found = propertyRepository.findById(saved.uuid());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
        assertEquals(saved.propertyCode(), found.get().propertyCode());
    }

    @Test
    void findByCode_returnsEmpty_whenNotFound() {
        Optional<PropertyRow> found = propertyRepository.findByCode("NOPE99");

        assertTrue(found.isEmpty());
    }

    @Test
    void findAll_returnsAllSavedProperties() {
        // Arrange
        propertyRepository.save("Test Mobile Park", "999 Test Ave", "TP");
        propertyRepository.save("Test Mobile Park2", "1001 Test Ave", "TP2");

        // Act
        List<PropertyRow> all = propertyRepository.findAll();

        // Assert
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().allMatch(p -> p.deletedAt() == null));
    }

    @Test
    void findById_returnsNull_onSoftDeletedProperties() {
        // Arrange
        PropertyRow saved = propertyRepository.save("Test Mobile Park", "999 Test Ave", "TP");
        boolean deleteSuccess = propertyRepository.softDelete(saved.uuid());

        // Act
        PropertyRow found = propertyRepository.findById(saved.uuid()).orElse(null);

        // Assert
        assertTrue(deleteSuccess);
        assertNull(found);
    }
}






