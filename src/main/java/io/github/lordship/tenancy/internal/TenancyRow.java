package io.github.lordship.tenancy.internal;

import io.github.lordship.tenancy.Tenancy;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public record TenancyRow(
    UUID uuid,
    UUID lotNumber,
    UUID accountNumber,
    Date startDate,
    Date endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {


    public Tenancy toTenancy(){
        return new Tenancy(
                this.uuid,
                this.lotNumber,
                this.accountNumber,
                this.startDate,
                this.endDate,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    public static TenancyRow forInsert(
            UUID lotNumber,
            UUID accountNumber,
            Date startDate,
            Date endDate
    ) {
        return new TenancyRow(
                null,
                lotNumber,
                accountNumber,
                startDate,
                endDate,
                null,
                null,
                null
        );
    }

}
