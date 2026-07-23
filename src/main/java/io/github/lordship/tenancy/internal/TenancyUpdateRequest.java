package io.github.lordship.tenancy.internal;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record TenancyUpdateRequest(
        @NotNull
        UUID uuid,

        UUID lotId,

        LocalDate startDate,

        LocalDate endDate

        //        String status
) {
}