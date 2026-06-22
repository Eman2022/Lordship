package io.github.lordship.audit;

import io.github.lordship.shared.UserType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogRow(
        UUID uuid,
        UUID correlationId,
        UUID userId,
        UserType userType,
        String ipAddress,
        String tableName,
        String recordId,
        OperationType operation,
        String valueBefore,
        String valueAfter,
        LocalDateTime changedAt
) {

    public AuditLogRow(UUID correlationId, UUID userId, UserType userType,
                       String ipAddress, String tableName, String recordId,
                       OperationType operation, String valueBefore, String valueAfter) {
        this(
                null,
                correlationId,
                userId,
                userType,
                ipAddress,
                tableName,
                recordId,
                operation,
                valueBefore,
                valueAfter,
                null
        );
    }
}