package io.github.lordship.persons.internal;

import jakarta.validation.constraints.NotBlank;

public record PersonCreateRequest(
    @NotBlank
    String nameFirst,

    @NotBlank
    String nameLast
) {
}