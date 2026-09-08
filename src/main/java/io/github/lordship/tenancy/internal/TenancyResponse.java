package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;
import java.time.LocalDate;
import java.util.UUID;

public record TenancyResponse(
        UUID uuid,
        UUID lotId,
        LocalDate startDate,
        LocalDate endDate
) {

    public static TenancyResponse from(Tenancy t) {
        return new TenancyResponse(
                t.uuid(),
                t.lotId(),
                t.startDate(),
                t.endDate()
        );
    }

}