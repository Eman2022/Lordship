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
import java.time.format.DateTimeParseException;
import java.util.*;

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
    public Optional<Tenant> patchTenant(UUID uuid, Map<String, Object> changes) {
        Optional<TenantRow> beforeOpt = tenantRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }

        TenantRow before = beforeOpt.get();
        Map<String, Object> mutable = new HashMap<>(changes);

        if (mutable.containsKey("start_date")) {
            Object raw = mutable.get("start_date");
            try {
                if (raw instanceof String s && !s.isBlank()) {
                    LocalDate parsed = LocalDate.parse(s);

                    if (Objects.equals(before.startDate(), parsed)) {
                        mutable.remove("start_date");
                    } else {
                        mutable.put("start_date", parsed);
                    }

                } else {
                    // Will skip if not edited
                    if (before.startDate() == null) {
                        mutable.remove("start_date");
                    } else {
                        mutable.put("start_date", null);
                    }
                }
            } catch(DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date"); // Throws error if updated date is not valid
            }
        }

        if (mutable.containsKey("end_date")) {
            Object raw = mutable.get("end_date");

            try {
                if (raw instanceof String s && !s.isBlank()) {
                    LocalDate parsed = LocalDate.parse(s);

                    if (Objects.equals(before.endDate(), parsed)) {
                        mutable.remove("end_date");
                    } else {
                        mutable.put("end_date", parsed);
                    }

                } else {
                    if (before.endDate() == null) {
                        mutable.remove("end_date");
                    } else {
                        mutable.put("end_date", null);
                    }
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date"); // Same as startDate
            }
        }

        if(mutable.isEmpty()) {
            return Optional.of(before.toTenant());
        }

        Optional<TenantRow> afterOpt = tenantRepository.patch(uuid, mutable);
        if (afterOpt.isEmpty()) return Optional.empty();

        TenantRow after = afterOpt.get();

        var diff = AuditMapper.diff(before, after);
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("tenant", uuid, diff.before(), diff.after());
        }

        return Optional.of(after.toTenant());
    }

    @Transactional
    public boolean softDelete(UUID uuid) {
        return tenantRepository.findById(uuid).map(tenant -> {
            tenantRepository.softDelete(uuid);
            auditService.recordDelete("tenancy", uuid, AuditMapper.toMap(tenant));
            return true;
        }).orElse(false);
    }
}
