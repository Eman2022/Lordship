package io.github.lordship.lots;

import io.github.lordship.shared.AgreementType;

import java.math.BigDecimal;

public record PermissibleAgreementType(
        AgreementType agreementType,
        BigDecimal targetRent
) {
}