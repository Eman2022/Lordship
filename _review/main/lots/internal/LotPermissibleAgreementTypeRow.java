package io.github.lordship.lots.internal;

import io.github.lordship.shared.AgreementType;

import java.math.BigDecimal;
import java.util.UUID;

public record LotPermissibleAgreementTypeRow(
        UUID lotId,
        AgreementType agreementType,
        BigDecimal targetRent
) {
}