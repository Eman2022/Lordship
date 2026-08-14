package io.github.lordship.identity;

import java.util.UUID;

public record TenantPrincipal(
        UUID tenantUuid,
        UUID personUuid
) implements LordshipPrincipal {

}
