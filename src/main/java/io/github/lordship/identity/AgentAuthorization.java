package io.github.lordship.identity;

import io.github.lordship.shared.PropertyScope;

import java.time.OffsetDateTime;
import java.util.Set;

public record AgentAuthorization(
        AgentPrincipal principal,
        OffsetDateTime tokensValidFrom,
        Set<String> permissions
) {

    public AgentAuthorization {
        permissions = Set.copyOf(permissions);

        // has to branch b/c the
        if (principal.propertyScope() instanceof PropertyScope.Restricted(var propertyIds)) {
            principal = new AgentPrincipal(
              principal.agentUuid(),
              principal.personUuid(),
              new PropertyScope.Restricted(Set.copyOf(propertyIds))
            );
        }
    }
}
