package io.github.lordship.lots;

import io.github.lordship.shared.AgreementType;

import java.math.BigDecimal;

/**
 * targetRate is what we're moving existing tenants toward
 * askingRate is the ask for new tenants
 */
public record PermissibleAgreementType(
        AgreementType agreementType,
        BigDecimal targetRate,
        BigDecimal askingRate
) {
}