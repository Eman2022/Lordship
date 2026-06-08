package io.github.lordship.access.internal;


import io.github.lordship.access.AgentWithPerson;

import java.util.UUID;

public record AgentRegistrationResponse(UUID uuid,
                                        String workEmail,
                                        String fullName
) {

    public static AgentRegistrationResponse from(AgentWithPerson agentWithPerson){
        return new AgentRegistrationResponse(
                agentWithPerson.agent().uuid(),
                agentWithPerson.agent().workEmail(),
                agentWithPerson.person().fullName()
        );
    }
}
