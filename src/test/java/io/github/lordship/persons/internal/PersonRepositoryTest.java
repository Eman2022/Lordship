package io.github.lordship.persons.internal;

import io.github.lordship.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class PersonRepositoryTest extends IntegrationTest {

    @Autowired
    PersonRepository personRepository;

    private PersonRow buildRow() {
        return new PersonRow("Don Mock");
    }

    private PersonRow buildRowWithEmail(String email) {
        return new PersonRow(
          null, "Don Mock",
          null, null, email, null,
          null, null, null, null
        );
    }

    @Test
    void save_shouldPersistRow_andReturnGeneratedFields() {
        // Arrange
        String personName = "Don Mock";

        // Act
        PersonRow saved = personRepository.save(personName);

        // Assert
        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        assertEquals("Don Mock", saved.nameFull());
    }


    @Test
    void save_shouldSetCreatedAt_toRoughlyNow(){
        // Arrange
        String nameFull = "Don Mock";

        // Act
        PersonRow saved = personRepository.save(nameFull);

        // Assert   OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS),
        Duration age = Duration.between(saved.createdAt(), OffsetDateTime.now(ZoneOffset.UTC));
        assertTrue(age.toSeconds() < 5, "expecting to be created within 5s");
    }

    @Test
    void patch_shouldThrow_whenEmergencyContactDoesNotExist(){
        // Arrange
        String nameFull = "Don Mock";
        PersonRow saved = personRepository.save(nameFull);

        // Act and Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> personRepository.patch(saved.uuid(), Map.of("emergency_contact", UUID.randomUUID()))
        );
    }

    @Test
    void findById_shouldReturnRow_whenExists() {
        // Arrange
        String nameFull = "Don Mock";
        PersonRow saved = personRepository.save(nameFull);

        // Act
        Optional<PersonRow> found = personRepository.findById(saved.uuid());

        // Assert
        assertTrue(found.isPresent());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        // Arrange
           // (nothing)
        // Act
        Optional<PersonRow> found = personRepository.findById(UUID.randomUUID());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findById_shouldReturnEmpty_whenRowIsSoftDeleted() {
        // Arrange
        String nameFull = "Don Mock";
        PersonRow saved = personRepository.save(nameFull);
        personRepository.softDelete(saved.uuid());

        // Act
        Optional<PersonRow> found = personRepository.findById(saved.uuid());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_shouldMatch_caseInsensitively() {
        // Arrange
        String nameFull = "Don Mock";
        PersonRow saved = personRepository.save(nameFull);
        String emailAddress = "DonMock@lordship.com";
        personRepository.patch(saved.uuid(), Map.of("personal_email", emailAddress));

        // Act
        Optional<PersonRow> found = personRepository.findByEmail(emailAddress.toLowerCase());

        // Assert
        assertTrue(found.isPresent());
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenSoftDeleted() {
        // Arrange
        PersonRow saved = personRepository.save("Don Mock");
        String emailAddress = "DonMock@lordship.com";
        personRepository.patch(saved.uuid(), Map.of("personal_email", emailAddress));
        personRepository.softDelete(saved.uuid());

        // Act
        Optional<PersonRow> found = personRepository.findByEmail("DonMock@lordship.com");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void patch_shouldUpdateField_andReturnUpdatedRow() {
        // Arrange
        PersonRow rowSaved = personRepository.save("Don Mock");
        PersonRow emergencyContactRowSaved = personRepository.save("Don Mock's Friend");
        Map<String, Object> changes = Map.of(
                "name_full","Baby Mock",
                "personal_email", "BabyMock@lordship.com",
                "birthday", LocalDate.parse("1970-07-01"),
                "personal_phone", "599-211-2121",
                "mailing_address", "69805 NF-9041, North Bend, WA 98045",
                "social", "123-45-6789",
                "emergency_contact", emergencyContactRowSaved.uuid()
                );

        // Act
        Optional<PersonRow> patched = personRepository.patch(rowSaved.uuid(), changes);

        // Assert
        assertTrue(patched.isPresent());
        PersonRow updatedRow = patched.get();
        assertEquals("Baby Mock", updatedRow.nameFull());
        assertEquals("BabyMock@lordship.com", updatedRow.personalEmail());
        assertEquals(LocalDate.of(1970, 7, 1), updatedRow.birthday());
        assertEquals("599-211-2121", updatedRow.personalPhone());
        assertEquals("69805 NF-9041, North Bend, WA 98045", updatedRow.mailingAddress());
        assertEquals("123-45-6789", updatedRow.social());
        assertEquals(emergencyContactRowSaved.uuid(), updatedRow.emergencyContact());
    }

    @Test
    void patch_shouldReturnUnchangedRow_withEmptyChanges() {
        // Arrange
        PersonRow emergencyContactRow = personRepository.save("Don Mock's Friend");
        PersonRow row = personRepository.save("Don Mock");
        PersonRow rowSaved = personRepository.patch(row.uuid(), Map.of("emergency_contact", emergencyContactRow.uuid())).orElse(null);
        Map<String, Object> changes = Map.of();

        // Act
        Optional<PersonRow> patchedOpt = personRepository.patch(rowSaved.uuid(), changes);

        // Assert
        assertTrue(patchedOpt.isPresent());
        PersonRow patchedRow = patchedOpt.get();
        assertEquals(rowSaved.uuid(), patchedRow.uuid());
        assertEquals(rowSaved.nameFull(), patchedRow.nameFull());
        assertEquals(rowSaved.birthday(), patchedRow.birthday());
        assertEquals(rowSaved.personalPhone(), patchedRow.personalPhone());
        assertEquals(rowSaved.personalEmail(), patchedRow.personalEmail());
        assertEquals(rowSaved.social(), patchedRow.social());
        assertEquals(rowSaved.createdAt(), patchedRow.createdAt());
        assertEquals(emergencyContactRow.uuid(), patchedRow.emergencyContact());
    }

    @Test
    void patch_shouldThrow_whenColumnIsNotAllowed() {
        // Arrange
        PersonRow row = personRepository.save("Don Mock");

        // Act & Assert
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
            personRepository.patch(row.uuid(), Map.of("middle_name", "Kipper"))
        );

        // Act & Assert
        assertThrows(InvalidDataAccessApiUsageException.class, () ->
                personRepository.patch(row.uuid(), Map.of("uuid", UUID.randomUUID()))
        );
    }

    @Test
    void patch_shouldReturnEmpty_whenRowIsSoftDeleted() {
        // Arrange
        PersonRow row = personRepository.save("Don Mock");
        personRepository.softDelete(row.uuid());

        // Act
        Optional<PersonRow> patched = personRepository.patch(row.uuid(), Map.of("name_full", "Gone"));

        // Assert
        assertFalse(patched.isPresent());
    }
}