package io.github.lordship.chargeterms.internal;

import io.github.lordship.chargeterms.ChargeTerm;
import io.github.lordship.chargeterms.ChargeTermSource;
import io.github.lordship.chargeterms.ChargeTermStatus;
import io.github.lordship.chargeterms.LateFeeMethod;
import io.github.lordship.chargeterms.UtilityMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ChargeTermRow(
        UUID uuid,
        UUID tenancy,
        LocalDate validAt,

        BigDecimal rentAmount,
        BigDecimal carFee,
        int allowedCars,
        BigDecimal petFee,
        int allowedPets,

        int rentDueDay,
        int gracePeriodDays,

        LateFeeMethod lateFeeMethod,
        BigDecimal lateFeeAmount,
        BigDecimal lateFeeMax,

        UtilityMethod waterMethod,
        BigDecimal waterFlatAmount,
        UtilityMethod powerMethod,
        BigDecimal powerFlatAmount,
        UtilityMethod sewerMethod,
        BigDecimal sewerFlatAmount,
        UtilityMethod trashMethod,
        BigDecimal trashFlatAmount,

        ChargeTermStatus status,
        ChargeTermSource source,
        UUID sourceUuid,

        OffsetDateTime cancelledAt,
        UUID cancelledBy,
        String cancelReason,

        OffsetDateTime deletedAt,
        String note,
        OffsetDateTime createdAt,
        UUID createdBy
) {

    public ChargeTerm toChargeTerm() {
        return new ChargeTerm(
                uuid, tenancy, validAt,
                rentAmount, carFee, allowedCars, petFee, allowedPets,
                rentDueDay, gracePeriodDays,
                lateFeeMethod, lateFeeAmount, lateFeeMax,
                waterMethod, waterFlatAmount,
                powerMethod, powerFlatAmount,
                sewerMethod, sewerFlatAmount,
                trashMethod, trashFlatAmount,
                status, source, sourceUuid,
                cancelledAt, cancelledBy, cancelReason,
                deletedAt, note, createdAt, createdBy
        );
    }

    /**
     * A brand new term always starts PROPOSED with no paper attached. Status and
     * sourceUuid are deliberately not parameters -- you cannot create a term that
     * is already in force, which is what forces every deal through the pipeline.
     */
    public static ChargeTermRow forInsert (
            UUID tenancy,
            LocalDate validAt,
            BigDecimal rentAmount,
            BigDecimal carFee,

            int allowedCars,
            BigDecimal petFee,
            int allowedPets,
            int rentDueDay,
            int gracePeriodDays,

            LateFeeMethod lateFeeMethod,
            BigDecimal lateFeeAmount,
            BigDecimal lateFeeMax,

            UtilityMethod waterMethod,
            BigDecimal waterFlatAmount,
            UtilityMethod powerMethod,
            BigDecimal powerFlatAmount,
            UtilityMethod sewerMethod,
            BigDecimal sewerFlatAmount,
            UtilityMethod trashMethod,
            BigDecimal trashFlatAmount,
            ChargeTermSource source,
            String note,
            UUID createdBy
    ) {
        return new ChargeTermRow(
                null, tenancy, validAt,
                rentAmount, carFee, allowedCars, petFee, allowedPets,
                rentDueDay, gracePeriodDays,
                lateFeeMethod, lateFeeAmount, lateFeeMax,
                waterMethod, waterFlatAmount,
                powerMethod, powerFlatAmount,
                sewerMethod, sewerFlatAmount,
                trashMethod, trashFlatAmount,
                ChargeTermStatus.PROPOSED, source, null,
                null, null, null,
                null, note, null, createdBy
        );
    }
}