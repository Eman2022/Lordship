package io.github.lordship.persons;

import io.github.lordship.audit.AuditService;
import io.github.lordship.persons.internal.PersonRepository;
import io.github.lordship.persons.internal.PersonRow;
import io.github.lordship.shared.EncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
public class PersonServiceTest {

    @Mock
    PersonRepository personRepository;

    @Mock
    EncryptionService encryptionService;

    @Mock
    AuditService auditService;

    @InjectMocks
    PersonService personService;

    @Test
    void createPersonFromName_shouldReturnPersonWithCorrectName_andRecordAudit(){
        // Arrange
        String nameFull = "Don Mock";

        PersonRow stubRow = new PersonRow(
                UUID.randomUUID(), nameFull,
                null, null, null, null, null, null,
                LocalDateTime.now(), null
        );

        when(personRepository.save(any())).thenReturn(stubRow);

        // Act
        Person result = personService.createPersonFromName(nameFull);

        // Assert
        assertEquals("Don Mock", result.nameFull());
        assertNotNull(result.uuid());
        verify(auditService).recordInsert(eq("person"), eq(stubRow.uuid()), any());
    }

    @Test
    void findById_shouldReturnPerson_whenFound() {
        // Arrange
        PersonRow stubRow = new PersonRow(
                UUID.randomUUID(), "Don Mock",
                null, null, null, null, null, null,
                LocalDateTime.now(), null
        );
        when(personRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));

        // Act
        Optional<Person> result = personService.findByID(stubRow.uuid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(stubRow.uuid(), result.get().uuid());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();
        when(personRepository.findById(unknownUUID)).thenReturn(Optional.empty());

        // Act
        Optional<Person> result = personService.findByID(unknownUUID);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(auditService);
    }

    @Test
    void deletePerson_shouldReturnTrue_andRecordAudit_whenPersonExists() {
        // Arrange
        PersonRow stubRow = new PersonRow(
                UUID.randomUUID(), "Don Mock",
                null, null, null, null, null, null,
                LocalDateTime.now(), null
        );
        when(personRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));

        // Act
        boolean result = personService.deletePerson(stubRow.uuid());

        // Assert
        assertTrue(result);
        verify(personRepository).softDelete(stubRow.uuid());
        verify(auditService).recordDelete(eq("person"), eq(stubRow.uuid()), any());
    }

    @Test
    void deletePerson_shouldReturnFalse_andNotRecordAudit_whenPersonDoesNotExist() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();
        when(personRepository.findById(unknownUUID)).thenReturn(Optional.empty());

        // Act
        boolean result = personService.deletePerson(unknownUUID);

        // Assert
        assertFalse(result);
        verify(personRepository, never()).softDelete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void patchPerson_shouldReturnEmpty_whenPersonDoesNotExists() {
        // Arrange
        UUID unknownUUID = UUID.randomUUID();
        when(personRepository.findById(unknownUUID)).thenReturn(Optional.empty());

        // Act
        Optional<Person> result = personService.patchPerson(unknownUUID, Map.of("name_first", "Zoe"));

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(auditService);
    }

    @Test
    void patchPerson_shouldNotRecordAudit_whenNoDiffProduced(){
        // Arrange
        PersonRow stubRow = new PersonRow(
                UUID.randomUUID(), "Don Mock",
                null, null, null, null, null, null,
                LocalDateTime.now(), null
        );
        when(personRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));
        // prep a patch that doesn't change anything
        when(personRepository.patch(eq(stubRow.uuid()), any())).thenReturn(Optional.of(stubRow));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("name_full", "Don Mock");
        personService.patchPerson(stubRow.uuid(), changes);

        // Assert
        verifyNoInteractions(auditService);
    }

    @Test
    void patchPerson_shouldRecordAudit_whenFieldActuallyChanges() {
        // Arrange
        PersonRow before = new PersonRow(
                UUID.randomUUID(), "Don Mock",
                null, null, null,null, null, null,
                LocalDateTime.now(), null
        );
        PersonRow after = new PersonRow(
                before.uuid(), "Baby Mocko",
                null, null, null, null, null, null,
                before.createdAt(), null
        );
        when(personRepository.findById(before.uuid())).thenReturn(Optional.of(before));
        when(personRepository.patch(eq(before.uuid()), any())).thenReturn(Optional.of(after));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("name_full", "Baby Mocko");
        personService.patchPerson(before.uuid(), changes);

        // Assert
        verify(auditService).recordUpdate(eq("person"), eq(before.uuid()), any(), any());
    }

    @Test
    void patchPerson_shouldEncryptSocial_beforePassingToRepository() {
        // Arrange
        PersonRow stubRow = new PersonRow(
                UUID.randomUUID(), "Don Mock",
                null, null, null, null, null, null,
                LocalDateTime.now(), null
        );
        PersonRow afterRow = new PersonRow(
                stubRow.uuid(), "Don Mock",
                null, null, null, null, null, "ENCRYPTED",
                stubRow.createdAt(), null
        );
        when(personRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));
        when(encryptionService.encrypt("123-45-6789")).thenReturn("ENCRYPTED");
        when(personRepository.patch(eq(stubRow.uuid()), any())).thenReturn(Optional.of(afterRow));
        when(encryptionService.decrypt("ENCRYPTED")).thenReturn("123-45-6789");

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("social", "123-45-6789");
        personService.patchPerson(stubRow.uuid(), changes);

        // Assert
        verify(encryptionService).encrypt("123-45-6789");
        // make sure the repo received the encrypted value, not the plaintext
        verify(personRepository).patch(eq(stubRow.uuid()), argThat(
                map -> "ENCRYPTED".equals(map.get("social")))
        );
    }

    @Test
    void patchPerson_shouldParseBirthdayString_toLocalDate() {
        // Arrange
        PersonRow stubRow = new PersonRow(
                UUID.randomUUID(), "Don Mock",
                null, null, null, null, null, null,
                LocalDateTime.now(), null
        );
        PersonRow afterRow = new PersonRow(
                stubRow.uuid(), "Don Mock",
                LocalDate.of(1990, 12, 17), null, null, null, null, "ENCRYPTED",
                stubRow.createdAt(), null
        );
        when(personRepository.findById(stubRow.uuid())).thenReturn(Optional.of(stubRow));
        when(personRepository.patch(eq(stubRow.uuid()), any())).thenReturn(Optional.of(afterRow));

        // Act
        Map<String, Object> changes = new HashMap<>();
        changes.put("birthday", "1990-12-17");
        personService.patchPerson(stubRow.uuid(), changes);

        // Assert
        verify(personRepository).patch(eq(stubRow.uuid()), argThat(
                map -> LocalDate.of(1990, 12, 17).equals(map.get("birthday"))
        ));
    }
}
