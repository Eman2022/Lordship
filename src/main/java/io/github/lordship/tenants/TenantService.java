package io.github.lordship.tenants;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.tenants.internal.TenantCreateRequest;
import io.github.lordship.tenants.internal.TenantRepository;
import io.github.lordship.tenants.internal.TenantRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantService
{
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    public TenantService(
            TenantRepository tenantRepository,
            AuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Tenant create(TenantCreateRequest request) {
        List<TenantRow> active = tenantRepository.findByTenancy(request.tenancyId());
        active.forEach(t -> tenantRepository.end(t.uuid(), LocalDate.now()));

        TenantRow row = tenantRepository.save(
                TenantRow.forInsert(
                        request.tenancyId(),
                        request.personId(),
                        LocalDate.now(),
                        null
                )
        );

        auditService.recordInsert("tenant", row.uuid(), AuditMapper.toMap(row));
        return row.toTenant();
    }

    public Optional<Tenant> findById(UUID uuid) {
        return tenantRepository.findById(uuid).map(TenantRow::toTenant);
    }

    @Transactional
    public boolean delete(UUID uuid) {
        return tenantRepository.findById(uuid).map(before -> {
            tenantRepository.softDelete(uuid);
            auditService.recordDelete("tenant", uuid, AuditMapper.toMap(before));
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<Tenant> patch(UUID uuid, Map<String, Object> changes) {
        Optional<TenantRow> beforeOpt = tenantRepository.findById(uuid);
        if (beforeOpt.isEmpty()) return Optional.empty();

        TenantRow before = beforeOpt.get();
        Optional<TenantRow> afterOpt = tenantRepository.patch(uuid, changes);
        if (afterOpt.isEmpty()) return Optional.empty();

        TenantRow after = afterOpt.get();

        var diff = AuditMapper.diff(before, after);
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("tenant", uuid, diff.before(), diff.after());
        }

        return Optional.of(after.toTenant());
    }
}
