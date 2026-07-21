package io.github.lordship.propertyassignments.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PropertyAssignmentRepository {

    private final JdbcClient jdbc;

    public PropertyAssignmentRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public PropertyAssignmentRow save(PropertyAssignmentRow row) {
        return jdbc.sql("""
                INSERT INTO agent_property_assignment (
                    agent_id, property_id, assigned_by
                ) VALUES (
                    :agentId, :propertyId, :assignedBy
                ) RETURNING *
                """).paramSource(row)
                .query(PropertyAssignmentRow.class)
                .single();
    }

    public Optional<PropertyAssignmentRow> findById(UUID assignmentId) {
        return jdbc.sql("""
                SELECT * FROM agent_property_assignment
                WHERE uuid = :assignmentId AND removed_at IS NULL
                """)
                .param("assignmentId", assignmentId)
                .query(PropertyAssignmentRow.class)
                .optional();
    }

    public Set<PropertyAssignmentRow> getActivePropertyAssignmentsForAgent(UUID agentId) {
        return jdbc.sql("""
                SELECT uuid, agent_id, property_id, assigned_by, assigned_at, removed_at
                FROM agent_property_assignment
                WHERE agent_id = :agentId AND removed_at IS NULL
                """)
                .param("agentId", agentId)
                .query(PropertyAssignmentRow.class)
                .set();
    }

    public void softDelete(UUID uuid) {
        jdbc.sql("""
                UPDATE agent_property_assignment
                SET removed_at = now()
                WHERE uuid = :uuid AND removed_at IS NULL
        """)
        .param("uuid", uuid)
        .update();
    }

}
