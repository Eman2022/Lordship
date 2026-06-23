package io.github.lordship.lots;

import java.time.LocalDateTime;
import java.util.UUID;

public record Lot(
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
  public boolean isSoftDeleted() {
    return deletedAt != null;
  }
}
