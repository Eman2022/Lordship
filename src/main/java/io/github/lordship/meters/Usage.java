package io.github.lordship.meters;

import java.util.UUID;

public record Usage(
        UUID meterId,
        MeterRead startRead,
        MeterRead endRead,
        double usage
) {
}
