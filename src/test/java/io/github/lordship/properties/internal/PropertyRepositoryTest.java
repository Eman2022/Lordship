package io.github.lordship.properties.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional

public class PropertyRepositoryTest {
    @Autowired
    PropertyRepository propertyRepository;

    private PropertyRow buildRow() {
        return new PropertyRow(
                null,
                "TST" + UUID.randomUUID().toString().substring(0, 5).toUpperCase(),
                "Test Mobile Park",
                "999 Test Ave",
                null,
                null,
                null,
                null
        );
    }

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {
        PropertyRow saved = propertyRepository.save(buildRow());

        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        assertEquals("Test Mobile Park", saved.propertyName());
        assertEquals("999 Test Ave", saved.propertyAddress());
    }

    @Test
    void findByPropertyCodeReturnsSavedProperty() {
        PropertyRow saved = propertyRepository.save(buildRow());

        Optional<PropertyRow> found = propertyRepository.getPropertyOptional(saved.propertyCode());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
        assertEquals(saved.propertyCode(), found.get().propertyCode());
    }

    @Test
    void findByPropertyCodeReturnsEmptyWhenNotFound() {
        Optional<PropertyRow> found = propertyRepository.getPropertyOptional("NOPE99");

        assertTrue(found.isEmpty());
    }

    @Test
    void findAllReturnsAllSavedProperties() {
        propertyRepository.save(buildRow());
        propertyRepository.save(buildRow());

        List<PropertyRow> all = propertyRepository.findAll();

        assertTrue(all.size() >= 2);
        assertTrue(all.stream().allMatch(p -> p.deletedAt() == null));
    }
}
   // @Test
   // void softDeletedPropertyDoesNotAppearInFindAll() {
   //     PropertyRow saved = propertyRepository.save(buildRow());

   //     propertyRepository.softDelete(saved.propertyCode());

   //     List<PropertyRow> all = propertyRepository.findAll();
   //     assertTrue(all.stream().noneMatch(p -> p.propertyCode().equals(saved.propertyCode())));
   // }

   // @Test
   // void softDeletedPropertyDoesNotAppearInFindByCode() {
   //     PropertyRow saved = propertyRepository.save(buildRow());

   //     propertyRepository.softDelete(saved.propertyCode());

   //     Optional<PropertyRow> found = propertyRepository.getPropertyOptional(saved.propertyCode());
   //     assertTrue(found.isEmpty());
   // }
// }


// @Test
// void findAllDoesNotReturnSoftDeletedProperties() {


    // NOTE: PropertyRepository currently has no soft-delete method.
    // This test documents expected behavior for findAll()'s existing
    // "WHERE deleted_at IS NULL" filter once a delete method exists.
    // If/when you add a softDelete(UUID) method to PropertyRepository,
    // this test should be updated to actually exercise it rather than
    // just asserting the filter clause is present in findAll().



//    PropertyRow saved = propertyRepository.save(buildRow());

//    List<PropertyRow> all = propertyRepository.findAll();

//    assertTrue(all.stream().anyMatch(p -> p.uuid().equals(saved.uuid())));






