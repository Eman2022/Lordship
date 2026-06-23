package io.github.lordship.audit.internal;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.lordship.audit.AuditLog;
import io.github.lordship.audit.OperationType;
import io.github.lordship.shared.EncryptionService;
import io.github.lordship.shared.UserType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private static final Set<String> SENSITIVE_FIELDS = Set.of("social");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String decryptJson(String json, EncryptionService encryptionService) {
        if (json == null) return null;

        try {
            Map<String, Object> map = MAPPER.readValue(json, new TypeReference<>() {});
            Map<String, Object> result = new HashMap<>();

            for (var entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String str && SENSITIVE_FIELDS.contains(key)) {
                    result.put(key, encryptionService.decrypt(str));
                } else {
                    result.put(key, value);
                }
            }
            return MAPPER.writeValueAsString(result);
        } catch (Exception e){
            throw new RuntimeException("Failed to decrypt audit JSON", e);
        }
    }

    public AuditLog toAuditLog(EncryptionService encryptionService) {
        return new AuditLog(
                uuid,
                correlationId,
                userId,
                userType,
                ipAddress,
                tableName,
                recordId,
                operation,
                decryptJson(this.valueBefore, encryptionService),
                decryptJson(this.valueAfter, encryptionService),
                changedAt
        );
    }
}