package io.github.lordship.tenants.internal;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record TenantCreateRequest(
        @NotNull
        UUID tenancyId,

        @NotNull
        UUID personId,

        // Optional, ISO yyyy-MM-dd. Omitted, the service picks the billing period
        // the office is working in -- see TenantService.defaultStartDate.
        LocalDate startDate
) { }
