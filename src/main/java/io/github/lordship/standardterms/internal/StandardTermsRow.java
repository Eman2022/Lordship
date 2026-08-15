package io.github.lordship.standardterms.internal;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.standardterms.StandardTerms;
import io.github.lordship.shared.UtilityMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StandardTermsRow(
        UUID uuid,
        UUID property,
        String name,
        AgreementType agreementType,
        BigDecimal targetRate,

        BigDecimal carFee,
        Integer allowedCars,

        BigDecimal petFee,
        Integer allowedPets,

        Integer rentDueDay,
        Integer gracePeriodDays,

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
    public StandardTerms toStandardTerms() {
        return new StandardTerms(
                uuid, property, name, agreementType, targetRate,
                carFee, allowedCars, petFee, allowedPets,
                rentDueDay, gracePeriodDays,
                ruleViolationFeeMethod, ruleViolationFeeAmount,
                nsfFeeMethod, nsfFeeAmount,
                lateFeeMethod, lateFeeAmount,
                waterMethod, waterFlatAmount,
                powerMethod, powerFlatAmount,
                sewerMethod, sewerFlatAmount,
                trashMethod, trashFlatAmount,
                note, createdAt, updatedAt, updatedBy, deletedAt
        );
    }

    // Minimal insert -- every other column has a DB default.
    public StandardTermsRow(UUID property, String name, AgreementType agreementType) {
        this(null, property, name, agreementType, null,
                null, null, null, null,
                null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    // Copies a global template into a property, keeping the terms and dropping identity.
    public StandardTermsRow copyTo(UUID targetProperty) {
        return new StandardTermsRow(
                null, targetProperty, name, agreementType, targetRate,
                carFee, allowedCars, petFee, allowedPets,
                rentDueDay, gracePeriodDays,
                ruleViolationFeeMethod, ruleViolationFeeAmount,
                nsfFeeMethod, nsfFeeAmount,
                lateFeeMethod, lateFeeAmount,
                waterMethod, waterFlatAmount,
                powerMethod, powerFlatAmount,
                sewerMethod, sewerFlatAmount,
                trashMethod, trashFlatAmount,
                note, null, null, null, null
        );
    }
}