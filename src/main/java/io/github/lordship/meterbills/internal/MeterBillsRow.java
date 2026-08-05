package io.github.lordship.meterbills.internal;


import java.util.UUID;

public record MeterBillsRow(
        UUID billedMeter,
        Integer billedAmount
) {
}
