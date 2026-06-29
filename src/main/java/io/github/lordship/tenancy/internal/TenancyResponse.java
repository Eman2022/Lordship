package io.github.lordship.tenancy.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.lordship.tenancy.Tenancy;

import java.time.LocalDate;
import java.util.UUID;

@JsonFormat
public record TenancyResponse(
        UUID uuid,
        UUID lotID,
        LocalDate startDate,
        LocalDate endDate
) {

    public static TenancyResponse from(Tenancy t) {
        return new TenancyResponse(
                t.uuid(),
                t.lotID(),
                t.startDate(),
                t.endDate()
        );
    }

}