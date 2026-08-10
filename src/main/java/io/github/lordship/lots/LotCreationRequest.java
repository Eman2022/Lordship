package io.github.lordship.lots;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LotCreationRequest(

        @NotNull
        UUID propertyId,

        @NotBlank
        String lotNumber,

        String description,

        Double targetRent,

        String notes,

        Integer sortOrder

) {
}
