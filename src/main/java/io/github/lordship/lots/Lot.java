package io.github.lordship.lots;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Lot(
    UUID uuid,
    UUID propertyId,
    String lotNumber,
    String description,
    String notes,
    Integer sortOrder,
    OffsetDateTime createdAt,
    OffsetDateTime deletedAt
) {
  public boolean isSoftDeleted() {
    return deletedAt != null;
  }
}
