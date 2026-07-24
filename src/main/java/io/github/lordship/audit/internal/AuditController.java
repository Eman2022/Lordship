package io.github.lordship.audit.internal;


import io.github.lordship.audit.AuditLog;
import io.github.lordship.audit.AuditService;
import io.github.lordship.shared.PageResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditlogs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("hasAuthority('audit:view')")
    @GetMapping("/byagent/{uuid}")
    public ResponseEntity<PageResult<AuditLogResponse>> findAgentAuditLogs(
            @PathVariable UUID uuid,
            @Valid AgentAuditLogPageRequest request,
            Authentication authentication) {

        PageResult<AuditLog> result = auditService.findAgentAuditLogs(
                uuid,
                request.page(),
                request.pageSize(),
                request.sortBy(),
                request.ascending()
        );

        PageResult<AuditLogResponse> mapped = result.map(log ->
                AuditLogResponse.from(log, authentication)
        );

        return ResponseEntity.ok(mapped);
    }
}
