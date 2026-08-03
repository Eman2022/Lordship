package io.github.lordship.persons.internal;

import io.github.lordship.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        PersonRow row = buildRow();

        // Act
        PersonRow saved = personRepository.save(row);

        // Assert
        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
        assertEquals("Don Mock", saved.nameFull());
    }

    @Test
    void save_shouldPersistAllFields_whenAllProvided(){
        // Arrange
        PersonRow emergencyContactSaved =  personRepository.save(buildRow());
        PersonRow fullRow = new PersonRow(
                null, "Baby Mock",
                LocalDate.of(1970, 7, 1), "599-211-2121", "BabyMock@lordship.com",
                "69805 NF-9041, North Bend, WA 98045", emergencyContactSaved.uuid(),
                "123-45-6789", null, null
        );

        // Act
        PersonRow saved = personRepository.save(fullRow);

        // Assert
        assertNotNull(saved.uuid());
        assertEquals("Baby Mock", saved.nameFull());
        assertEquals(LocalDate.of(1970, 7, 1), saved.birthday());
        assertEquals("599-211-2121", saved.personalPhone());
        assertEquals("BabyMock@lordship.com", saved.personalEmail());
        assertEquals("69805 NF-9041, North Bend, WA 98045", saved.mailingAddress());
        assertEquals(emergencyContactSaved.uuid(), saved.emergencyContact());
        assertEquals("123-45-6789", saved.social());
    }

    @Test
    void save_shouldSetCreatedAt_toRoughlyNow(){
        // Arrange
        PersonRow row = buildRow();

        // Act
        PersonRow saved = personRepository.save(row);

        // Assert
        Duration age = Duration.between(saved.createdAt(), LocalDateTime.now()).abs();
        assertTrue(age.toSeconds() < 5, "expecting to be created within 5s");
    }

    @Test
    void save_shouldThrow_whenEmergencyContactDoesNotExist(){
        // Arrange
        PersonRow row = new PersonRow(null,"Don Mock",
                null, null, null, null,
                UUID.randomUUID(),
                null, null, null);

        // Act and Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> personRepository.save(row)
        );
    }

    @Test
    void findById_shouldReturnRow_whenExists() {
        // Arrange
        PersonRow row = buildRow();
        PersonRow saved = personRepository.save(row);

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
        PersonRow row = buildRow();
        PersonRow saved = personRepository.save(row);
        personRepository.softDelete(saved.uuid());

        // Act
        Optional<PersonRow> found = personRepository.findById(saved.uuid());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_shouldMatch_caseInsensitively() {
        // Arrange
        String emailAddress = "DonMock@lordship.com";
        PersonRow row = buildRowWithEmail(emailAddress);
        personRepository.save(row);

        // Act
        Optional<PersonRow> found = personRepository.findByEmail(emailAddress.toLowerCase());

        // Assert
        assertTrue(found.isPresent());
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenSoftDeleted() {
        // Arrange
        PersonRow row = buildRowWithEmail("DonMock@lordship.com");
        PersonRow saved = personRepository.save(row);
        personRepository.softDelete(saved.uuid());

        // Act
        Optional<PersonRow> found = personRepository.findByEmail("DonMock@lordship.com");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void patch_shouldUpdateField_andReturnUpdatedRow() {
        // Arrange
        PersonRow row = buildRow();
        PersonRow emergencyContactRow = buildRow();
        PersonRow rowSaved = personRepository.save(row);
        PersonRow emergencyContactRowSaved = personRepository.save(emergencyContactRow);
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
        PersonRow emergencyContactRow = personRepository.save(buildRow());
        PersonRow row = personRepository.save(buildRow());
        PersonRow rowSaved = personRepository.patch(row.uuid(), Map.of("emergency_contact", emergencyContactRow.uuid())).get();
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
        PersonRow row = personRepository.save(buildRow());

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
        PersonRow row = personRepository.save(buildRow());
        personRepository.softDelete(row.uuid());

        // Act
        Optional<PersonRow> patched = personRepository.patch(row.uuid(), Map.of("name_full", "Gone"));

        // Assert
        assertFalse(patched.isPresent());
    }
}