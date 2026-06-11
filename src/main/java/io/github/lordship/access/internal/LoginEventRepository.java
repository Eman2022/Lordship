package io.github.lordship.access.internal;


import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class LoginEventRepository {

    private final JdbcClient jdbc;

    public LoginEventRepository(JdbcClient jdbcClient) {
        this.jdbc = jdbcClient;
    }

    public LoginEventRow save(LoginEventRow loginEventRow) {

        return jdbc.sql("""
                INSERT INTO login_event (agent_id, occurred_at, ip_address, browser_client, browserOs, outcome)
                VALUES (:agentId, :occurredAt, :ipAddress, :browserClient, :browserOs, :outcome)
                RETURNING *
                """)
                .paramSource(loginEventRow)
                .query(LoginEventRow.class)
                .single();
    }

    public List<LoginEventRow> getLoginEventsByAgentId(UUID agentId) {
        return jdbc.sql("""
                    SELECT *
                    FROM login_event
                    WHERE agent_id = :agentId
                    ORDER BY occurred_at DESC
                    """)
                .param("agentId", agentId)
                .query(LoginEventRow.class)
                .list();
    }

}


//CREATE TABLE login_event (
//        uuid UUID PRIMARY KEY DEFAULT uuidv7(),
//agent_id UUID NOT NULL,
//occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//ip_address VARCHAR(45),
//browser_client TEXT, -- what did they use to log in?
//outcome SMALLINT NOT NULL, -- use an HTTP status code
//FOREIGN KEY (agent_id) REFERENCES agent(uuid)
//        );