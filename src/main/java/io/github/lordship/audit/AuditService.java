package io.github.lordship.audit;

import io.github.lordship.audit.internal.AuditLogResponse;
import io.github.lordship.audit.internal.AuditLogRow;
import io.github.lordship.audit.internal.AuditRepository;
import io.github.lordship.shared.PageRequest;
import io.github.lordship.shared.PageResult;
import io.github.lordship.shared.UserType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final ObjectMapper objectMapper;
    private final AuditContext auditContext;
    private final AuditRepository auditRepository;

    public AuditService(AuditContext auditContext, ObjectMapper objectMapper, AuditRepository auditRepository) {
        this.auditContext = auditContext;
        this.objectMapper = objectMapper;
        this.auditRepository = auditRepository;
    }

    public void recordInsert(String tableName, UUID recordId, Map<String, Object> after) {
        var row = new AuditLogRow(
                auditContext.getCorrelationId(),
                auditContext.getActingUserId(),
                auditContext.getUserType(),
                auditContext.getIpAddress(),
                tableName,
                recordId.toString(),
                OperationType.INSERT,
                null,
                toJson(after)
        );
        auditRepository.save(row);
    }

    public void recordSystemInsert(UUID correlationId, String tableName, UUID recordId, Map<String, Object> after) {
        AuditLogRow row = new AuditLogRow(
                correlationId,
                null,
                UserType.SYSTEM,
                "system",
                tableName,
                recordId.toString(),
                OperationType.INSERT,
                null,
                toJson(after)
        );
        auditRepository.save(row);
    }

    public void recordUpdate(String tableName, UUID recordId, Map<String, Object> before, Map<String, Object> after) {
        var row = new AuditLogRow(
                auditContext.getCorrelationId(),
                auditContext.getActingUserId(),
                auditContext.getUserType(),
                auditContext.getIpAddress(),
                tableName,
                recordId.toString(),
                OperationType.UPDATE,
                toJson(before),
                toJson(after)
        );
        auditRepository.save(row);
    }

    public void recordDelete(String tableName, UUID recordId, Map<String, Object> before) {
        var row = new AuditLogRow(
                auditContext.getCorrelationId(),
                auditContext.getActingUserId(),
                auditContext.getUserType(),
                auditContext.getIpAddress(),
                tableName,
                recordId.toString(),
                OperationType.DELETE,
                toJson(before),
                null
        );
        auditRepository.save(row);
    }

    public PageResult<AuditLog> findAgentAuditLogs(UUID agentId, Integer page, Integer pageSize, String sortBy, boolean ascending) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, sortBy, ascending);
        return auditRepository.findAllAgentAuditLogs(agentId, pageRequest);
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(map);
    }
}