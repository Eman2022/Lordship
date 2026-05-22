package io.github.lordship.access;

import java.util.UUID;

public record AgentResponse(

        UUID uuid,
        String workEmail,
        String fullName,
        String token
)

{
    public static AgentResponse from(AgentWithPerson agentWithPerson, String token) {
        return new AgentResponse(
                agentWithPerson.agent().uuid(),
                agentWithPerson.agent().workEmail(),
                agentWithPerson.person().fullName(),
                token
        );
    }

}
