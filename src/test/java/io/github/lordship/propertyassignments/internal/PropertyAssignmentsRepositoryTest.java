package io.github.lordship.propertyassignments.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.access.internal.AgentRepository;
import io.github.lordship.access.internal.AgentRow;

import io.github.lordship.persons.internal.PersonRepository;
import io.github.lordship.persons.internal.PersonRow;
import io.github.lordship.properties.internal.PropertyRepository;
import io.github.lordship.properties.internal.PropertyRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class PropertyAssignmentsRepositoryTest extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Autowired
    PropertyAssignmentRepository propertyAssignmentRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    AgentRepository agentRepository;

    @Autowired
    PropertyRepository propertyRepository;

    Random random = new Random();

    private AgentRow buildAgent() {
        PersonRow personRow = new PersonRow("Some Guy");
        PersonRow personRowSaved = personRepository.save(personRow);
        return agentRepository.save(new AgentRow(personRowSaved.uuid(), "", "workEmail" + String.valueOf(random.nextInt(999999)) + "@Lordship.com", "supergoodNicePass123123,"));
    }


    private PropertyRow buildProperty() {
        PropertyRow propertyRow = new PropertyRow("Test Property", "2161 Pretty Ave, Tacoma WA 91234");
        return propertyRepository.save(propertyRow);
    }

    private PropertyAssignmentRow buildRow() {
        Optional<AgentRow> rootAgentRowOpt = agentRepository.findByWorkEmail(rootEmail);
        assertTrue(rootAgentRowOpt.isPresent());
        AgentRow rootAgentRow = rootAgentRowOpt.get();
        PropertyRow propertyRow = buildProperty();
        AgentRow agentRow = buildAgent();
        return new PropertyAssignmentRow(agentRow.uuid(), propertyRow.uuid(), rootAgentRow.uuid());
    }


    @Test
    void save_shouldPersistRow_andReturnGeneratedFields() {
        //Arrange
        PropertyAssignmentRow propertyAssignmentRow = buildRow();

        // Act
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(propertyAssignmentRow);
        Duration age = Duration.between(saved.assignedAt(), LocalDateTime.now()).abs();

        // Assert
        assertNotNull(saved.uuid());
        assertNotNull(saved.assignedAt());
        assertEquals(propertyAssignmentRow.propertyId(), saved.propertyId());
        assertEquals(propertyAssignmentRow.agentId(), saved.agentId());
        assertEquals(propertyAssignmentRow.assignedBy(), saved.assignedBy());
        assertTrue(age.toSeconds() < 10, "expecting to be created within 5s");
    }

    @Test
    void save_shouldThrow_whenAssignmentExists() {
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());
        PropertyAssignmentRow duplicate = new PropertyAssignmentRow(saved.agentId(), saved.propertyId(), saved.assignedBy());

        // Act and Assert
        assertThrows(DataIntegrityViolationException.class, () -> propertyAssignmentRepository.save(duplicate));
    }

    @Test
    void save_shouldPersistDuplicate_afterLastAssignmentEnds() {
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());
        PropertyAssignmentRow duplicate = new PropertyAssignmentRow(saved.agentId(), saved.propertyId(), saved.assignedBy());
        propertyAssignmentRepository.endAssignment(saved.uuid());

        // Act
        PropertyAssignmentRow duplicateSaved = propertyAssignmentRepository.save(duplicate);

        // Assert
        assertNotNull(duplicateSaved.uuid());
    }

    @Test
    void save_shouldThrow_whenAgentDoesNotExist() {
        // Arrange
        PropertyAssignmentRow propertyAssignmentRow = buildRow();
        PropertyAssignmentRow row = new PropertyAssignmentRow(UUID.randomUUID(), propertyAssignmentRow.uuid(), propertyAssignmentRow.uuid());

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> propertyAssignmentRepository.save(row));
    }

    @Test
    void save_shouldThrow_whenPropertyDoesNotExist() {
        // Arrange
        PropertyAssignmentRow propertyAssignmentRow = buildRow();
        PropertyAssignmentRow row = new PropertyAssignmentRow(propertyAssignmentRow.agentId(), UUID.randomUUID(), propertyAssignmentRow.uuid());

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> propertyAssignmentRepository.save(row));
    }

    @Test
    void save_shouldThrow_whenAssignorDoesNotExist() {
        // Arrange
        PropertyAssignmentRow propertyAssignmentRow = buildRow();
        PropertyAssignmentRow row = new PropertyAssignmentRow(propertyAssignmentRow.agentId(), propertyAssignmentRow.propertyId(),UUID.randomUUID());

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> propertyAssignmentRepository.save(row));
    }

    @Test
    void findById_shouldReturnRow_whenExists() {
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());

        // Act
        Optional<PropertyAssignmentRow> found = propertyAssignmentRepository.findById(saved.uuid());

        // Assert
        assertTrue(found.isPresent());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        // Act
        Optional<PropertyAssignmentRow> found = propertyAssignmentRepository.findById(UUID.randomUUID());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findById_shouldReturnEmpty_whenAssignmentEnded() {
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());
        propertyAssignmentRepository.endAssignment(saved.uuid());

        // Act
        Optional<PropertyAssignmentRow> found = propertyAssignmentRepository.findById(saved.uuid());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void getAgentActiveAssignments_shouldReturnAssignment_whenActive() {
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());

        // Act
        Set<PropertyAssignmentRow> found = propertyAssignmentRepository.getAgentActiveAssignments(saved.agentId());

        // Assert
        assertTrue(found.contains(saved));
    }

    @Test
    void getActiveAssignments_shouldExcludeAssignment_afterEnding() {
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());
        propertyAssignmentRepository.endAssignment(saved.uuid());

        // Act
        Set<PropertyAssignmentRow> found = propertyAssignmentRepository.getAgentActiveAssignments(saved.agentId());

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void getActiveAssignments_shouldNotInclude_otherAgentsAssignment(){
        // Arrange
        PropertyAssignmentRow saved = propertyAssignmentRepository.save(buildRow());
        PropertyAssignmentRow otherAgentAssignment = propertyAssignmentRepository.save(buildRow());

        // Act
        Set<PropertyAssignmentRow> found = propertyAssignmentRepository.getAgentActiveAssignments(saved.agentId());

        // Assert
        assertEquals(1, found.size());
        assertTrue(found.contains(saved));
        assertFalse(found.contains(otherAgentAssignment));
    }
}
