package io.github.lordship.lots.internal;

import io.github.lordship.shared.AgreementType;

import java.math.BigDecimal;
import java.util.UUID;

// Component order matches the column order in V5__lots.sql.
public record LotPermissibleAgreementTypeRow(
        UUID uuid,
        UUID lotId,
        AgreementType agreementType,
        BigDecimal targetRate, // null until somebody prices this kind of deal on this lot
        BigDecimal askingRate
) {
    /** A row on its way in: the database assigns the uuid. */
    public LotPermissibleAgreementTypeRow(UUID lotId, AgreementType agreementType, BigDecimal targetRate, BigDecimal askingRate) {
        this(null, lotId, agreementType, targetRate, askingRate);
    }
}