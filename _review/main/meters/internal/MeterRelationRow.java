package io.github.lordship.meters.internal;

import io.github.lordship.meters.MeterRead;
import io.github.lordship.meters.MeterRelation;

import java.time.LocalDate;
import java.util.UUID;

public record MeterRelationRow(
        UUID uuid,
        UUID parentMeter,
        UUID childMeter,
        Boolean hasUnmeteredRemainder,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
    public MeterRelation toMeterRelation(){
        return new MeterRelation(
                this.uuid,
                this.parentMeter,
                this.childMeter,
                this.hasUnmeteredRemainder,
                this.effectiveFrom,
                this.effectiveTo
        );
    }

    public static MeterRelationRow forInsert(
            UUID parentMeter,
            UUID childMeter,
            Boolean hasUnmeteredRemainder,
            LocalDate effectiveFrom
    ) {
        return new MeterRelationRow(
                null,
                parentMeter,
                childMeter,
                hasUnmeteredRemainder,
                effectiveFrom,
                null);
    }
}