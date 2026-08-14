package io.github.lordship.access.internal.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreationRequest(

        @NotBlank
        @Size(max = 60)
        String roleName,

        String roleDescription

) {
}