package io.github.lordship.documenttemplate.internal;

import io.github.lordship.shared.AgreementType;
import io.github.lordship.shared.InstrumentType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * agreement_type and instrument_type are Postgres enum types, which the default
 * property mapper reads as objects rather than Java enums -- those two columns
 * are the only reason this file exists.
 */
public class PropertyDocumentAssignmentRowMapper implements RowMapper<PropertyDocumentAssignmentRow> {

    @Override
    public PropertyDocumentAssignmentRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PropertyDocumentAssignmentRow(
                rs.getObject("uuid", UUID.class),
                rs.getObject("property", UUID.class),
                rs.getObject("document_template", UUID.class),
                AgreementType.valueOf(rs.getString("agreement_type")),
                InstrumentType.valueOf(rs.getString("instrument_type")),
                rs.getString("note"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("created_by", UUID.class),
                rs.getObject("deleted_at", OffsetDateTime.class)
        );
    }
}