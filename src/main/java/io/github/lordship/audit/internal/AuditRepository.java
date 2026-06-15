package io.github.lordship.audit.internal;


import io.github.lordship.shared.UserType;
import io.github.lordship.audit.AuditLogRow;
import io.github.lordship.audit.OperationType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;


@Repository
public class AuditRepository {

    private final JdbcClient jdbc;

    public AuditRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AuditLogRow> ROW_MAPPER = (rs, rowNum) -> new AuditLogRow(
            rs.getObject("uuid", UUID.class),
            rs.getObject("correlation_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            UserType.valueOf(rs.getString("user_type")),
            rs.getString("ip_address"),
            rs.getString("table_name"),
            rs.getString("record_id"),
            OperationType.valueOf(rs.getString("operation")),
            rs.getString("value_before"),
            rs.getString("value_after"),
            rs.getObject("changed_at", LocalDateTime.class)
    );

    public AuditLogRow save(AuditLogRow auditLogRow) {
        return jdbc.sql("""
                insert into audit_log (
                    correlation_id, user_id, user_type, ip_address,
                    table_name, record_id, operation, value_before, value_after
                )
                values (
                    :correlationId, :userId, :userType::user_type, :ipAddress,
                    :tableName, :recordId, :operation::operation_type, :valueBefore, :valueAfter
                )
                RETURNING *
                """)
                .param("correlationId", auditLogRow.correlationId())
                .param("userId", auditLogRow.userId())
                .param("userType", auditLogRow.userType().name())
                .param("ipAddress", auditLogRow.ipAddress())
                .param("tableName", auditLogRow.tableName())
                .param("recordId", auditLogRow.recordId())
                .param("operation", auditLogRow.operation().name())
                .param("valueBefore", auditLogRow.valueBefore())
                .param("valueAfter", auditLogRow.valueAfter())
                .query(ROW_MAPPER)
                .single();
    }
}
