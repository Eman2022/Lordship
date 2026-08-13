package io.github.lordship.meters.internal;

import io.github.lordship.meters.MeterRead;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadRow(
        UUID uuid,
        UUID targetedMeter,
        Integer meterAmount,
        OffsetDateTime readAt,
        Boolean isEstimated,
        Integer rolloverCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    public MeterRead toMeterRead(){
        return new MeterRead(
                this.uuid,
                this.targetedMeter,
                this.meterAmount,
                this.readAt,
                this.isEstimated,
                this.rolloverCount,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    public static MeterReadRow forInsert(
            UUID targetedMeter,
            Integer meterAmount,
            OffsetDateTime readAt,
            Boolean isEstimated,
            Integer rolloverCount
    ) {
        return new MeterReadRow(
                null,
                targetedMeter,
                meterAmount,
                readAt,
                isEstimated,
                rolloverCount,
                null,
                null,
                null
        );
    }
}