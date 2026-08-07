package io.github.lordship.access.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        @Size(min = 12, max = 64, message = "Password must be between 12 and 64 characters")
        String newPassword
) {

    @Override
    public String toString() {
        return "ChangePasswordRequest";
    }
}
