package io.github.lordship.meters.internal;

import io.github.lordship.meters.Usage;
import java.util.UUID;

public record UsageResponse(
        UUID meterId,
        MeterReadResponse startRead,
        MeterReadResponse endRead,
        double usage
) {
    public static UsageResponse from(Usage usage) {
        return new UsageResponse(
                usage.meterId(),
                MeterReadResponse.from(usage.startRead()),
                MeterReadResponse.from(usage.endRead()),
                usage.usage()
        );
    }
}