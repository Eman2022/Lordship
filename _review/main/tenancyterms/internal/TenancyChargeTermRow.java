package io.github.lordship.tenancyterms.internal;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.FeeMethod;
import io.github.lordship.shared.UtilityMethod;
import io.github.lordship.termstemplate.TermsTemplate;
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

        BigDecimal rate, // COALESCE(lot target_rate for this type, terms_template.target_rate)
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

        UtilityMethod trashMethod, // no SUBMETERED for trash
        BigDecimal trashFlatAmount,

        TenancyTermStatus status,
        TenancyTermSource source,
        UUID sourceUuid,     // the instrument that produced this deal
        UUID termsTemplate,  // which template seeded the values
        UUID batch,          // groups one bulk run

        OffsetDateTime cancelledAt,
        UUID cancelledBy,
        String cancelReason,
        OffsetDateTime deletedAt,

        String note,
        OffsetDateTime createdAt,
        UUID createdBy
) {

    // The initial term for a tenancy: every fee and method copied from the
    // property's template for this agreement type. Rate is resolved by the
    // caller, since it prefers the lot's target_rate over the template's.
    public static TenancyChargeTermRow fromTemplate(
            UUID tenancy,
            TermsTemplate template,
            BigDecimal rate,
            LocalDate validAt,
            TenancyTermSource source,
            UUID batch,
            UUID createdBy) {

        return new TenancyChargeTermRow(
                null,
                tenancy,
                validAt,
                template.agreementType(),
                rate,
                template.carFee(),
                template.allowedCars(),
                template.carsMax(),
                template.petFee(),
                template.allowedPets(),
                template.paymentDueDay(),
                template.gracePeriodDays(),
                template.ruleViolationFeeMethod(),
                template.ruleViolationFeeAmount(),
                template.nsfFeeMethod(),
                template.nsfFeeAmount(),
                template.lateFeeMethod(),
                template.lateFeeAmount(),
                template.waterMethod(),
                template.waterFlatAmount(),
                template.powerMethod(),
                template.powerFlatAmount(),
                template.sewerMethod(),
                template.sewerFlatAmount(),
                template.trashMethod(),
                template.trashFlatAmount(),
                TenancyTermStatus.PROPOSED,
                source,
                null,           // sourceUuid - no instrument until one is generated
                template.uuid(),
                batch,
                null, null, null, // cancelledAt, cancelledBy, cancelReason
                null,           // deletedAt
                null,           // note
                null,           // createdAt
                createdBy
        );
    }

    /** A single-term creation, outside any bulk run. */
    public static TenancyChargeTermRow fromTemplate(
            UUID tenancy,
            TermsTemplate template,
            BigDecimal rate,
            LocalDate validAt,
            TenancyTermSource source,
            UUID createdBy) {
        return fromTemplate(tenancy, template, rate, validAt, source, null, createdBy);
    }

    /** The day rent becomes late: due day plus the grace period. */
    public int lateAfterDay() {
        return paymentDueDay + gracePeriodDays;
    }

    public boolean isEditable() {
        return status.isEditable();
    }

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }
}