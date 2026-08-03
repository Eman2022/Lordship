package io.github.lordship.audit;

import io.github.lordship.shared.UserType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLog (
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
    OffsetDateTime changedAt
) {
}
