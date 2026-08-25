package io.github.lordship.lots.internal;

import io.github.lordship.shared.AgreementType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class LotPermissibleAgreementTypeRowMapper implements RowMapper<LotPermissibleAgreementTypeRow> {

    @Override
    public LotPermissibleAgreementTypeRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LotPermissibleAgreementTypeRow(
                (UUID) rs.getObject("lot_id"),
                AgreementType.valueOf(rs.getString("agreement_type")),
                rs.getBigDecimal("target_rate")
        );
    }
}