package io.github.lordship.identity;

import io.github.lordship.shared.PropertyScope;

import java.util.UUID;

public record AgentPrincipal (
        UUID agentUuid,
        UUID personUuid,
        PropertyScope propertyScope
) implements LordshipPrincipal { }
