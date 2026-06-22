package io.github.lordship.lots.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LotTypeRepository {

    private final JdbcClient jdbc;

    public LotTypeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<LotTypeRow> findAllActive() {
        return jdbc.sql("SELECT * FROM lot_type WHERE active = TRUE ORDER BY sort_order NULLS LAST, code")
                .query(LotTypeRow.class)
                .list();
    }

    public Optional<LotTypeRow> findByCode(String code) {
        return jdbc.sql("SELECT * FROM lot_type WHERE code = :code")
                .param("code", code)
                .query(LotTypeRow.class)
                .optional();
    }
}