package io.github.lordship.chargeterms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


 // A snapshot of the whole deal for one tenancy, taking effect on validAt
 // A change to any term is a NEW ChargeTerm, never an edit -- except while
 // ChargeTermStatus == PROPOSED

public record ChargeTerm (
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
    public boolean isEditable() {
        return status.isEditable() && deletedAt == null;
    }

     // returns true if this term governs the given billing period. Note this is NOT the
     // whole resolution rule -- a later ACTIVE term supersedes this one, which
     // only the repository query can determine.
    public boolean appliesOnOrAfter(LocalDate periodStart) {
        return status == ChargeTermStatus.ACTIVE
                && deletedAt == null
                && !validAt.isAfter(periodStart);
    }
}