package io.github.lordship.tenants.internal;

import io.github.lordship.tenants.Tenant;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantRow(
        UUID uuid,
        UUID tenancyId,
        UUID personId,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {

    public Tenant toTenant(){
        return new Tenant(
                this.uuid,
                this.tenancyId,
                this.personId,
                this.startDate,
                this.endDate,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    public static TenantRow forInsert(
            UUID tenancyId,
            UUID personId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new TenantRow(
                null,
                tenancyId,
                personId,
                startDate,
                endDate,
                null,
                null,
                null
        );
    }
}
