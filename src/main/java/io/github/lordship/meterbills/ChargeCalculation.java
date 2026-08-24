package io.github.lordship.meterbills;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ChargeCalculation(
        UUID lotMeter,
        UUID parentMeter,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal usageAmount,
        BigDecimal rateApplied,
        BigDecimal calculatedAmount,
        Boolean startReadEstimated,
        Boolean endReadEstimated
) {}
