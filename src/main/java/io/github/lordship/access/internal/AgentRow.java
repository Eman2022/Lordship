package io.github.lordship.access.internal;

import io.github.lordship.access.Agent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgentRow (
        UUID uuid,
        UUID personId,
        String workPhone,
        String workEmail,
        String agentPassword,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public Agent toAgent(){
        return new Agent(
                this.uuid,
                this.personId,
                this.workPhone,
                this.workEmail,
                this.createdAt,
                this.deletedAt
        );
    }

    public AgentRow(UUID personId, String workPhone, String workEmail, String agentPassword){
        this(
            null,
            personId,
            workPhone,
            workEmail,
            agentPassword,
            null,
            null
        );
    }
}