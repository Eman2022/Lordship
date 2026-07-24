package io.github.lordship.meters.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.lordship.meters.Meters;

import java.util.UUID;

@JsonFormat
public record MeterResponse(
        UUID uuid,
        UUID meterId,
        Double pointX,
        Double pointY
) {

    public static io.github.lordship.meters.internal.MeterResponse from(Meters m) {
        return new io.github.lordship.meters.internal.MeterResponse(
                m.uuid(),
                m.meterId(),
                m.pointX(),
                m.pointY()
        );
    }
}

/*             UUID meterId,
            String measurement,
            Double pointX,
            Double pointY */