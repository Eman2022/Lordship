package io.github.lordship.chargeterms.internal;


import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class ChargeTermRepository {

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "valid_at",
            "rent_amount", "rent_due_day", "grace_period_days",
            "car_fee", "allowed_cars",
            "pet_fee", "allowed_pets",
            "late_fee_method", "late_fee_amount", "late_fee_max",
            "water_method", "water_flat_amount",
            "power_method", "power_flat_amount",
            "sewer_method", "sewer_flat_amount",
            "trash_method", "trash_flat_amount",
            "source", "note"
    );

    private final JdbcClient jdbcClient;

    public ChargeTermRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

//    public ChargeTermRow save(ChargeTermRow row) {
//
//    }

}
