package io.github.lordship.standardterms.internal;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.shared.UtilityMethod;
import io.github.lordship.standardterms.StandardTerms;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public record StandardTermsResponse(
        UUID uuid,
        UUID property, // NOTE: A null property means this is a global template.
        String name,
        AgreementType agreementType,
        BigDecimal targetRate,

        BigDecimal carFee,
        int allowedCars,
        int carsMax,

        BigDecimal petFee,
        int allowedPets,

        int paymentDueDay,
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
        OffsetDateTime updatedAt
) {
    public static StandardTermsResponse from(StandardTerms terms) {
        return new StandardTermsResponse(
                terms.uuid(), terms.property(), terms.name(), terms.agreementType(), terms.targetRate(),
                terms.carFee(), terms.allowedCars(), terms.carsMax(), terms.petFee(), terms.allowedPets(),
                terms.paymentDueDay(), terms.gracePeriodDays(),
                terms.ruleViolationFeeMethod(), terms.ruleViolationFeeAmount(),
                terms.nsfFeeMethod(), terms.nsfFeeAmount(),
                terms.lateFeeMethod(), terms.lateFeeAmount(),
                terms.waterMethod(), terms.waterFlatAmount(),
                terms.powerMethod(), terms.powerFlatAmount(),
                terms.sewerMethod(), terms.sewerFlatAmount(),
                terms.trashMethod(), terms.trashFlatAmount(),
                terms.note(), terms.createdAt(), terms.updatedAt()
        );
    }
}