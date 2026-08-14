package io.github.lordship.access.internal.rbac;

import java.util.UUID;

public record GrantedRoleResponse(
        UUID uuid,
        UUID agentId,
        UUID roleId,
        UUID grantedBy)
{

}
