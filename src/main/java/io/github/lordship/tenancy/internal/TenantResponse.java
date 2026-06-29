package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenant;

import java.time.LocalDate;
import java.util.UUID;

public record TenantResponse(
        UUID uuid,
        UUID tenancyId,
        UUID personId,
        LocalDate startDate,
        LocalDate endDate
) {

    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
                t.uuid(),
                t.tenancyId(),
                t.personId(),
                t.startDate(),
                t.endDate()
        );
    }

}