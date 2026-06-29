package io.github.lordship.access.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GrantedRoleRequest(

        @NotNull
        UUID agentId,

        @NotBlank
        @Size(max = 60)
        String roleName
) { }
