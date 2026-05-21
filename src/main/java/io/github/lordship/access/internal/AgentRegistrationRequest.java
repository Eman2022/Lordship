package io.github.lordship.access.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRegistrationRequest (

    @NotBlank
    String nameFirst,

    @NotBlank
    String nameLast,

    @Email
    @NotBlank
    String personalEmail,

    @Email
    @NotBlank
    String workEmail,

    @Size(min = 7, max = 20, message = "Phone number must be between 7 and 20 characters")
    String workPhone,

    @NotBlank
    @Size(min = 12, message = "Password must be at least 12 characters")
    String password

) {}
