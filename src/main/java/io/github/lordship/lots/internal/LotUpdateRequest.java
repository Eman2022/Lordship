package io.github.lordship.lots.internal;

import jakarta.validation.constraints.NotBlank;

// Mutable fields of a lot. lot_number can change (renames are tracked in the
// audit log); property is intentionally not editable here -- moving a lot
// between properties is a separate, heavier operation.
public record LotUpdateRequest(

        @NotBlank
        String lotNumber,

        String description,

        String notes,

        //optional
        Integer sortOrder

) {
}
