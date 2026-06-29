package io.github.lordship.audit.internal;


import io.github.lordship.audit.AuditLog;
import io.github.lordship.shared.EncryptionService;
import io.github.lordship.shared.PageRequest;
import io.github.lordship.shared.PageResult;
import io.github.lordship.shared.UserType;
import io.github.lordship.audit.OperationType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Repository
public class AuditRepository {

    // allow-list of columns that may be used in ORDER BY; sortBy is request-controlled
    // and can't be bound as a normal param, so it must be validated before concatenation
    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "changed_at", "table_name", "record_id", "operation"
    );

    private final JdbcClient jdbc;
    private final EncryptionService encryptionService;

    public AuditRepository(JdbcClient jdbc,
                           EncryptionService encryptionService) {
        this.jdbc = jdbc;
        this.encryptionService = encryptionService;
    }

    private static final RowMapper<AuditLogRow> ROW_MAPPER = (rs, _) -> new AuditLogRow(
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

    public PageResult<AuditLog> findAllAgentAuditLogs(UUID agentId, PageRequest pageRequest) {
        if (!ALLOWED_SORT_COLUMNS.contains(pageRequest.sortBy())) {
            throw new IllegalArgumentException("Invalid sortBy column: " + pageRequest.sortBy());
        }
        String direction = pageRequest.ascending() ? "ASC" : "DESC";

        long total = jdbc.sql("""
                    SELECT count(*) FROM audit_log
                    WHERE user_id = :agentId
                    AND user_type = 'AGENT'
                    """)
                .param("agentId", agentId)
                .query(Long.class)
                .single();

        List<AuditLogRow> content = jdbc.sql("""
                    SELECT * FROM audit_log
                    WHERE user_id = :agentId
                    AND user_type = 'AGENT'
                    ORDER BY %s %s
                    LIMIT :limit OFFSET :offset
                    """.formatted(pageRequest.sortBy(), direction))
                .param("agentId", agentId)
                .param("limit", pageRequest.pageSize())
                .param("offset", pageRequest.offset())
                .query(ROW_MAPPER)
                .list();

        List<AuditLog> auditLogs = content.stream()
                .map(row -> row.toAuditLog(encryptionService))
                .toList();

        return PageResult.of(auditLogs, pageRequest, total);
    }
}
