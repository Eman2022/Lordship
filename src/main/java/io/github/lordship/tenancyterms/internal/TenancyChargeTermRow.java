package io.github.lordship.tenancyterms.internal;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.shared.UtilityMethod;
import io.github.lordship.standardterms.StandardTerms;
import io.github.lordship.tenancyterms.TenancyTermSource;
import io.github.lordship.tenancyterms.TenancyTermStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// Component order matches the column order in V9__documents_and_deals.sql.
public record TenancyChargeTermRow(
        UUID uuid,
        UUID tenancy,
        LocalDate validAt,

        AgreementType agreementType, // do not patch

        BigDecimal rate, // COALESCE(lot rate, standard_terms.target_rate)
        BigDecimal carFee,
        Integer allowedCars,
        Integer carsMax,
        BigDecimal petFee,
        Integer allowedPets,

        Integer paymentDueDay,
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

        TenancyTermStatus status,
        TenancyTermSource source,
        UUID sourceUuid,     // the instrument that produced this deal
        UUID standardTerms,  // which set seeded the values
        UUID batch,          // groups one bulk run

        OffsetDateTime cancelledAt,
        UUID cancelledBy,
        String cancelReason,
        OffsetDateTime deletedAt,

        String note,
        OffsetDateTime createdAt,
        UUID createdBy
) {


    //The initial term for a new tenancy: every fee and method copied from the property level standard term
    public static TenancyChargeTermRow fromStandardTerms(
            UUID tenancy,
            StandardTerms terms,
            BigDecimal rate,
            LocalDate validAt,
            TenancyTermSource source,
            UUID createdBy) {

        return new TenancyChargeTermRow(
                null,
                tenancy,
                validAt,
                terms.agreementType(),
                rate,
                terms.carFee(),
                terms.allowedCars(),
                terms.carsMax(),
                terms.petFee(),
                terms.allowedPets(),
                terms.paymentDueDay(),
                terms.gracePeriodDays(),
                terms.ruleViolationFeeMethod(),
                terms.ruleViolationFeeAmount(),
                terms.nsfFeeMethod(),
                terms.nsfFeeAmount(),
                terms.lateFeeMethod(),
                terms.lateFeeAmount(),
                terms.waterMethod(),
                terms.waterFlatAmount(),
                terms.powerMethod(),
                terms.powerFlatAmount(),
                terms.sewerMethod(),
                terms.sewerFlatAmount(),
                terms.trashMethod(),
                terms.trashFlatAmount(),
                TenancyTermStatus.PROPOSED,
                source,
                null,           // sourceUuid - no instrument until one is generated
                terms.uuid(),
                null,           // batch
                null, null, null, // cancelledAt, cancelledBy, cancelReason
                null,           // deletedAt
                null,           // note
                null,           // createdAt
                createdBy
        );
    }

    /** The day rent becomes late: due day plus the grace period. */
    public int lateAfterDay() {
        return paymentDueDay + gracePeriodDays;
    }

    public boolean isEditable() {
        return status == TenancyTermStatus.PROPOSED;
    }
}