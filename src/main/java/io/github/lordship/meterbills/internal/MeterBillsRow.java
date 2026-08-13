package io.github.lordship.meterbills.internal;


import io.github.lordship.meterbills.MeterBills;
import io.github.lordship.meters.MeterMeasurement;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterBillsRow(
        UUID uuid,
        UUID billedMeter,
        Integer billedAmount,
        Double rateAmount,
        MeterMeasurement rateUnit,
        LocalDate periodStart,
        LocalDate periodEnd,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    public MeterBills toMeterBills() {
        return new MeterBills(
                this.uuid,
                this.billedMeter,
                this.billedAmount,
                this.rateAmount,
                this.rateUnit,
                this.periodStart,
                this.periodEnd,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }
    public static MeterBillsRow forInsert(
            UUID billedMeter,
            int billedAmount,
            double rateAmount,
            MeterMeasurement rateUnit,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return new MeterBillsRow(
                null,
                billedMeter,
                billedAmount,
                rateAmount,
                rateUnit,
                periodStart,
                periodEnd,
                null,
                null,
                null
        );
    }
}
