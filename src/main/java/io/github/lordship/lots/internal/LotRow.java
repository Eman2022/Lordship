package io.github.lordship.lots.internal;

import io.github.lordship.lots.Lot;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LotRow(
        UUID uuid,
        UUID propertyId,
        String lotNumber,
        String description,
        String notes,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public Lot toLot() {
        return new Lot(
                this.uuid,
                this.propertyId,
                this.lotNumber,
                this.description,
                this.notes,
                this.sortOrder,
                this.createdAt,
                this.deletedAt
        );
    }

    // Convenience constructor for inserts (DB fills uuid, created_at).
    public LotRow(UUID propertyId, String lotNumber,
                  String description, String notes, Integer sortOrder) {
        this(null, propertyId, lotNumber, description, notes, sortOrder, null, null);
    }
}
