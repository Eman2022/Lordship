package io.github.lordship.documenttemplate.internal;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * agreement_type and instrument_type are Postgres enum types, which the default
 * property mapper reads as objects rather than Java enums -- so this file
 * exists for those two columns and nothing else.
 */
public class DocumentTemplateRowMapper implements RowMapper<DocumentTemplateRow> {

    @Override
    public DocumentTemplateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentTemplateRow(
                rs.getObject("uuid", java.util.UUID.class),
                rs.getString("name"),
                AgreementType.valueOf(rs.getString("agreement_type")),
                InstrumentType.valueOf(rs.getString("instrument_type")),
                rs.getInt("version"),
                rs.getString("note"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("created_by", java.util.UUID.class),
                rs.getObject("deleted_at", java.time.OffsetDateTime.class)
        );
    }
}