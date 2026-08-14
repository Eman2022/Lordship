package io.github.lordship.standardterms;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.shared.UtilityMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// One property's standard deal for one agreement type: the terms a new tenancy
// is created from. A row with a null property is a global template -- admin-only,
// never usable directly, copied into a property before it can be offered.
public record StandardTerms(
        UUID uuid,
        UUID property,
        String name,
        AgreementType agreementType,
        BigDecimal targetRate,

        BigDecimal carFee,
        int allowedCars,
        BigDecimal petFee,
        int allowedPets,

        int rentDueDay,
        int gracePeriodDays,

        FeeMethod ruleViolationFeeMethod,
        BigDecimal ruleViolationFeeAmount,

        FeeMethod nsfFeeMethod,
        BigDecimal nsfFeeAmount,

        FeeMethod lateFeeMethod,
        BigDecimal lateFeeAmount,

        UtilityMethod waterMethod,
        BigDecimal waterFlatAmount,
        UtilityMethod powerMethod,

        BigDecimal powerFlatAmount,
        UtilityMethod sewerMethod,

        BigDecimal sewerFlatAmount,
        UtilityMethod trashMethod,

        BigDecimal trashFlatAmount,

        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID updatedBy,
        OffsetDateTime deletedAt
) {
    public boolean isGlobalTemplate() {
        return property == null;
    }

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}