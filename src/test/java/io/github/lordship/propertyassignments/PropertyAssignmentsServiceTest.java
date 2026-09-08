package io.github.lordship.propertyassignments;

import io.github.lordship.audit.AuditService;
import io.github.lordship.identity.AgentAuthorizationCache;
import io.github.lordship.propertyassignments.internal.PropertyAssignmentRepository;
import io.github.lordship.propertyassignments.internal.PropertyAssignmentRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
public class PropertyAssignmentsServiceTest {

    @Mock
    PropertyAssignmentRepository propertyAssignmentRepository;

    @Mock
    AuditService auditService;

    @Mock
    AgentAuthorizationCache authorizationCache;

    @InjectMocks
    PropertyAssignmentService propertyAssignmentService;

    @Test
    void assignAgentToProperty_shouldReturnCorrectAssignment_andRecordAudit(){
        // Arrange
        UUID agentId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID assignedBy = UUID.randomUUID();

        PropertyAssignmentRow stubRow = new PropertyAssignmentRow(UUID.randomUUID(), agentId, propertyId, assignedBy,
                LocalDateTime.now(), null);

        when(propertyAssignmentRepository.save(any())).thenReturn(stubRow);

        //Act
        PropertyAssignment result = propertyAssignmentService.assign(agentId, propertyId, assignedBy);

        // Assert
        assertEquals(agentId, result.agentId());
        assertEquals(propertyId, result.propertyId());
        assertEquals(assignedBy, result.assignedBy());
        assertNotNull(result.uuid());
        verify(authorizationCache).invalidate(agentId);
        verify(auditService).recordInsert(eq("agent_property_assignment"), eq(stubRow.uuid()), any());
    }

    @Test
    void removeAssignment_shouldReturnTrue_andRecordAudit_whenFound() {
        // Arrange
        UUID assignmentId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID assignedBy = UUID.randomUUID();

        PropertyAssignmentRow stubRow = new PropertyAssignmentRow(assignmentId, agentId, propertyId, assignedBy,
                LocalDateTime.now(), null);

        when(propertyAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(stubRow));
        when(propertyAssignmentRepository.endAssignment(assignmentId)).thenReturn(true);

        // Act
        boolean result = propertyAssignmentService.endAssignment(assignmentId);

        // Assert
        assertTrue(result);
        verify(propertyAssignmentRepository).endAssignment(assignmentId);
        verify(auditService).recordDelete(eq("agent_property_assignment"), eq(stubRow.uuid()), any());
    }

    @Test
    void removeAssignment_shouldReturnFalse_andNotRecordAudit_whenNotFound() {
        // Arrange
        UUID assignmentId = UUID.randomUUID();
        when(propertyAssignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // Act
        boolean result = propertyAssignmentService.endAssignment(assignmentId);

        // Assert
        assertFalse(result);
        verify(propertyAssignmentRepository, never()).endAssignment(assignmentId);
        verifyNoInteractions(auditService);
    }

    @Test
    void getAgentAssignedProperties_shouldReturnCorrectSet_whenMultipleAssignments() {
        // Arrange
        UUID agentId2 = UUID.randomUUID(); // assignor id
        UUID agentId = UUID.randomUUID(); // active agent id
        UUID propertyId1 = UUID.randomUUID();
        UUID propertyId2 = UUID.randomUUID();
        Set<PropertyAssignmentRow> stubRows = Set.of(
          new PropertyAssignmentRow(UUID.randomUUID(), agentId, propertyId1, agentId2, LocalDateTime.of(2025, 2,1,0,0), null),
          new PropertyAssignmentRow(UUID.randomUUID(), agentId, propertyId2, agentId2, LocalDateTime.of(2025, 2,1,0,0), null)
        );
        when(propertyAssignmentRepository.getAgentActiveAssignments(agentId)).thenReturn(stubRows);

        // Act
        Set<UUID> results = propertyAssignmentService.getAgentAssignedPropertyUUIDs(agentId);

        // Assert
        assertEquals(2,results.size());
        assertTrue(results.contains(propertyId1));
        assertTrue(results.contains(propertyId2));
        verifyNoInteractions(auditService);
    }

    @Test
    void getAgentAssignedProperties_shouldReturnEmptySet_whenNoAssignments() {
        // Arrange
        UUID agentId = UUID.randomUUID();
        when(propertyAssignmentRepository.getAgentActiveAssignments(agentId)).thenReturn(Set.of());

        // Act
        Set<UUID> results = propertyAssignmentService.getAgentAssignedPropertyUUIDs(agentId);

        // Assert
        assertTrue(results.isEmpty());
        verifyNoInteractions(auditService);
    }
}
