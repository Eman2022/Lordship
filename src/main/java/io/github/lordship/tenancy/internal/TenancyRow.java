package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public record TenancyRow(
    UUID uuid,
    UUID lotId,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {


    public Tenancy toTenancy(){
        return new Tenancy(
                this.uuid,
                this.lotId,
                this.startDate,
                this.endDate,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    public static TenancyRow forInsert(
            UUID lotId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new TenancyRow(
                null,
                lotId,
                startDate,
                endDate,
                null,
                null,
                null
        );
    }

}
