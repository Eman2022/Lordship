package io.github.lordship.identity;

import java.util.UUID;

public sealed interface LordshipPrincipal permits AgentPrincipal, TenantPrincipal {
    UUID personUuid();
}
