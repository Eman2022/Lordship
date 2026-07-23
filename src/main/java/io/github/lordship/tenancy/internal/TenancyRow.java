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
    boolean noPersonalChecks,
    boolean noPartialPayments,
    boolean acceptPayments,
    boolean exemptFromLateFees,
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
                this.noPersonalChecks,
                this.noPartialPayments,
                this.acceptPayments,
                this.exemptFromLateFees,
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
                false,
                false,
                true,
                false,
                null,
                null,
                null
        );
    }

}
