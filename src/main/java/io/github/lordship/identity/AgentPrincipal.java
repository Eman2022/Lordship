package io.github.lordship.identity;

import java.util.UUID;

public record AgentPrincipal (
        UUID agentUuid,
        UUID personUuid
) implements LordshipPrincipal { }
