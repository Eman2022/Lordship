package io.github.lordship.access.internal;

import java.util.UUID;

public record GrantedRoleResponse(
        UUID uuid,
        UUID agentId,
        UUID roleId,
        UUID grantedBy)
{

}
