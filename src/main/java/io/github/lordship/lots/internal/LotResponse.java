package io.github.lordship.lots.internal;

import io.github.lordship.lots.Lot;
import java.util.UUID;

public record LotResponse(
        UUID uuid,
        UUID propertyId,
        String lotNumber,
        String description,
        String notes,
        Integer sortOrder
) {
    public static LotResponse from(Lot lot) {
        return new LotResponse(
                lot.uuid(),
                lot.propertyId(),
                lot.lotNumber(),
                lot.description(),
                lot.notes(),
                lot.sortOrder()
        );
    }
}
