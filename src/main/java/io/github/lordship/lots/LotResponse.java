package io.github.lordship.lots;

import java.util.UUID;

//move to internal package
public record LotResponse(
        UUID uuid,
        String propertyCode,
        String lotNumber,
        String lotTypeCode,
        String description,
        String notes,
        Integer sortOrder
) {
    public static LotResponse from(Lot lot) {
        return new LotResponse(
                lot.uuid(),
                lot.propertyCode(),
                lot.lotNumber(),
                lot.lotTypeCode(),
                lot.description(),
                lot.notes(),
                lot.sortOrder()
        );
    }
}
