package io.github.lordship.access;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AgentLoginRequest (

        @Email
        @NotBlank
        String workEmail,

        @NotBlank
        String password

) { }
