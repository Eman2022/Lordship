package io.github.lordship.lots;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LotCreationRequest(

        @NotBlank
        UUID propertyId,

        @NotBlank
        String lotNumber,

        @Size(min = 3, max = 3, message = "Lot type code must be exactly 3 characters")
        String lotTypeCode,

        String description,

        String notes,

        Integer sortOrder

) {
}
