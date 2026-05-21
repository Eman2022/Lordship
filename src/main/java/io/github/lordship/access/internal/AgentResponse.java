package io.github.lordship.access.internal;

import io.github.lordship.access.AgentWithPerson;

import java.util.UUID;

public record AgentResponse(

        UUID uuid,
        String workEmail,
        String fullName
)

{
    public static AgentResponse from(AgentWithPerson agentWithPerson) {
        return new AgentResponse(
                agentWithPerson.agent().uuid(),
                agentWithPerson.agent().workEmail(),
                agentWithPerson.person().fullName()
        );
    }


}
