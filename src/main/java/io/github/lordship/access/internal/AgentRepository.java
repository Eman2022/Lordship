package io.github.lordship.access.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentRepository {

    private final JdbcClient jdbc;

    public AgentRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public AgentRow save(AgentRow row) {
        return jdbc.sql("""
                INSERT INTO agent (
                        person_id, work_phone, work_email, agent_password
                    ) VALUES (
                        :personId, :workPhone, :workEmail, :agentPassword
                    ) RETURNING *
                """)
        .paramSource(row)
        .query(AgentRow.class)
        .single();
    }

    public Optional<AgentRow> findById(UUID uuid) {
        return jdbc.sql("SELECT * FROM agent WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(AgentRow.class)
                .optional();
    }

    public Optional<AgentRow> findByWorkEmail(String workEmail) {
        return jdbc.sql("SELECT * FROM agent WHERE work_email = :workEmail AND deleted_at IS NULL")
                .param("workEmail", workEmail)
                .query(AgentRow.class)
                .optional();
    }

}
