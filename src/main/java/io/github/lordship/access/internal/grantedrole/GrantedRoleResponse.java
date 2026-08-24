package io.github.lordship.access.internal.grantedrole;

import java.util.UUID;

public record GrantedRoleResponse(
        UUID uuid,
        UUID agentId,
        UUID roleId,
        UUID grantedBy)
{ }
