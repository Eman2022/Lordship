package io.github.lordship.propertyassignments;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.propertyassignments.internal.PropertyAssignmentRepository;
import io.github.lordship.propertyassignments.internal.PropertyAssignmentRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PropertyAssignmentService {

    private final PropertyAssignmentRepository propertyAssignmentRepository;
    private final AuditService auditService;

    public PropertyAssignmentService(PropertyAssignmentRepository propertyAssignmentRepository,
                                     AuditService auditService) {
        this.propertyAssignmentRepository = propertyAssignmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PropertyAssignment assign(UUID agentId, UUID propertyId, UUID assignedBy) {
        PropertyAssignmentRow row = propertyAssignmentRepository.save(
                new PropertyAssignmentRow(agentId, propertyId, assignedBy)
        );
        auditService.recordInsert("agent_property_assignment", row.uuid(), AuditMapper.toMap(row));
        return row.toPropertyAssignment();
    }

    @Transactional
    public boolean endAssignment(UUID assignmentId) {   // covers unassignAgentFromProperty
        return propertyAssignmentRepository.findById(assignmentId).map(assignment -> {
            if (!propertyAssignmentRepository.endAssignment(assignmentId)) {
                return false;
            }
            auditService.recordDelete("agent_property_assignment", assignmentId, AuditMapper.toMap(assignment));
            return true;
        }).orElse(false);
    }

    public Set<UUID> getAgentAssignedPropertyUUIDs(UUID agentId) {
        return propertyAssignmentRepository.getAgentActiveAssignments(agentId)
            .stream()
            .map(PropertyAssignmentRow::propertyId)
            .collect(Collectors.toSet());
    }
}