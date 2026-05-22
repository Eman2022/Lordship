package io.github.lordship.access.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GrantedRoleRepository {

    private final JdbcClient jdbc;

    public GrantedRoleRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public GrantedRoleRow save(GrantedRoleRow row) {
        return jdbc.sql("""
                INSERT INTO granted_role (agent_id, role_id, granted_by)
                VALUES (:agentId, :roleId, :grantedBy)
                RETURNING *
                """)
                .paramSource(row)
                .query(GrantedRoleRow.class)
                .single();
    }

    public List<GrantedRoleRow> findByAgentId(UUID agentId) {
        return jdbc.sql("""
                SELECT * FROM granted_role
                WHERE agent_id = :agentId
                AND deleted_at IS NULL
                """)
                .param("agentId", agentId)
                .query(GrantedRoleRow.class)
                .list();
    }


}

//CREATE TABLE granted_role (
//        uuid UUID PRIMARY KEY DEFAULT uuidv7(),
//agent_id UUID NOT NULL,
//role_id UUID NOT NULL,
//granted_by UUID NOT NULL,
//created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//deleted_at TIMESTAMP,
//FOREIGN KEY (agent_id) REFERENCES agent(uuid),
//FOREIGN KEY (role_id) REFERENCES agent_role(uuid),
//FOREIGN KEY (granted_by) REFERENCES agent(uuid)
//        );