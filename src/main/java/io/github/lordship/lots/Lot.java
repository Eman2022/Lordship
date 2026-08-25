package io.github.lordship.lots;

import io.github.lordship.shared.AgreementType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Lot(
        UUID uuid,
        UUID propertyId,
        Boolean isRentable,
        String notRentableReason,
        String lotNumber,
        String lotAddress,
        String lotParcel,
        String description,
        String notes,
        Integer sortOrder,
        ShapeData shapeData,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt,
        List<PermissibleAgreementType> permissibleAgreementTypes
) {
  public Lot { // note: this compact constructor
    permissibleAgreementTypes = List.copyOf(permissibleAgreementTypes);
  }

  public boolean isSoftDeleted() {
    return deletedAt != null;
  }

  public boolean permits(AgreementType agreementType) {
    return permissibleAgreementTypes.stream()
            .anyMatch(pat -> pat.agreementType() == agreementType);
  }

  public Optional<BigDecimal> targetRateFor(AgreementType agreementType) {
    return permissibleAgreementTypes.stream()
            .filter(pat -> pat.agreementType() == agreementType)
            .findFirst()
            .map(PermissibleAgreementType::targetRate);
  }
}