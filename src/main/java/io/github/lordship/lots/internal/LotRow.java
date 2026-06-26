package io.github.lordship.lots.internal;

import io.github.lordship.lots.Lot;

import java.time.LocalDateTime;
import java.util.UUID;

public record LotRow(
        UUID uuid,
        UUID propertyId,
        String lotNumber,
        String lotTypeCode,
        String description,
        String notes,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public Lot toLot() {
        return new Lot(
                this.uuid,
                this.propertyId,
                this.lotNumber,
                this.lotTypeCode,
                this.description,
                this.notes,
                this.sortOrder,
                this.createdAt,
                this.deletedAt
        );
    }

    // Convenience constructor for inserts (DB fills uuid, created_at).
    public LotRow(UUID propertyId, String lotNumber, String lotTypeCode,
                  String description, String notes, Integer sortOrder) {
        this(null, propertyId, lotNumber, lotTypeCode, description, notes, sortOrder, null, null);
    }
}
