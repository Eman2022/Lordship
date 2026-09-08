package io.github.lordship.tenants;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.tenancy.Tenancy;
import io.github.lordship.tenancy.TenancyService;
import io.github.lordship.tenants.internal.TenantCreateRequest;
import io.github.lordship.tenants.internal.TenantRepository;
import io.github.lordship.tenants.internal.TenantRow;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * A tenant row is one person's stay on one tenancy. A tenancy normally carries
 * several at once -- a household -- so adding one does nothing to the others.
 * The row that ends is the row of the person who left.
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenancyService tenancyService;
    private final AuditService auditService;

    public TenantService(
            TenantRepository tenantRepository,
            TenancyService tenancyService,
            AuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.tenancyService = tenancyService;
        this.auditService = auditService;
    }

    /**
     * The office is usually setting up next month's household, not today's, so an
     * omitted start_date lands on the first of a billing period rather than on
     * the date the row happened to be typed. Before the 10th the current period
     * is still the one in hand; after it, the work is for the period ahead.
     */
    static LocalDate defaultStartDate(LocalDate today) {
        return today.getDayOfMonth() < 10
                ? today.withDayOfMonth(1)
                : today.plusMonths(1).withDayOfMonth(1);
    }

    /**
     * Adds a person to a tenancy. The people already on it are untouched: a
     * spouse joining is not a move-out for anyone.
     *
     * <p>A person is on a tenancy once at a time, which is refused here for the
     * message and enforced by {@code uq_tenant_active_person} for the guarantee.
     * A person who moved out and moves back gets a second row rather than having
     * the first reopened, so the gap between the two stays stays visible.
     *
     * <p>An ended tenancy still admits a tenant: someone left off a household
     * that has since moved on is a correction the office has to be able to make.
     * A deleted or unknown tenancy does not.
     */
    @Transactional
    public Tenant create(TenantCreateRequest request) {
        Tenancy tenancy = tenancyService.findTenancyById(request.tenancyId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tenancy not found: " + request.tenancyId()));

        tenantRepository.findActiveByTenancyAndPerson(tenancy.uuid(), request.personId())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Person " + request.personId() + " is already an active tenant on tenancy "
                                    + tenancy.uuid() + " (tenant " + existing.uuid() + ")");
                });

        LocalDate startDate = request.startDate() != null
                ? request.startDate()
                : defaultStartDate(LocalDate.now());

        TenantRow row = tenantRepository.save(tenancy.uuid(), request.personId(), startDate);

        auditService.recordInsert("tenant", row.uuid(), AuditMapper.toMap(row));
        return row.toTenant();
    }

    public Optional<Tenant> findById(UUID uuid) {
        return tenantRepository.findById(uuid).map(TenantRow::toTenant);
    }

    /** Who is on this tenancy now. */
    public List<Tenant> findActiveByTenancy(UUID tenancyId) {
        return tenantRepository.findActiveByTenancy(tenancyId)
                .stream()
                .map(TenantRow::toTenant)
                .toList();
    }

    /** Every stay on this tenancy, ended ones included. */
    public List<Tenant> findByTenancy(UUID tenancyId) {
        return tenantRepository.findByTenancy(tenancyId)
                .stream()
                .map(TenantRow::toTenant)
                .toList();
    }

    /** Every tenancy this person has been on. */
    public List<Tenant> findByPerson(UUID personId) {
        return tenantRepository.findByPerson(personId)
                .stream()
                .map(TenantRow::toTenant)
                .toList();
    }

    /**
     * The one door onto a tenant's dates. There is no move-out endpoint: moving
     * out is setting end_date, so it comes through here, the same arrangement
     * TenancyService uses for ending a tenancy.
     *
     * <p>Null to a date is the move-out; date to a different date corrects one
     * typed wrong; a date back to null undoes a move-out entered by mistake, and
     * is refused when the person has since been added to the tenancy again --
     * without that check, clearing an end_date is a second way past
     * {@code uq_tenant_active_person}, which the create path never sees.
     *
     * <p>Mutable hashmap so the no-op keys can be dropped before the write.
     */
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
                    if (before.startDate() == null) {
                        mutable.remove("start_date");
                    } else {
                        mutable.put("start_date", null);
                    }
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date");
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
                throw new IllegalArgumentException("Invalid date");
            }
        }

        if (mutable.isEmpty()) {
            return Optional.of(before.toTenant());
        }

        // A key that survived the blocks above carries a real change; one that
        // did not means the supplied value already matched, so `before` is the
        // effective value either way.
        LocalDate startAfter = mutable.containsKey("start_date")
                ? (LocalDate) mutable.get("start_date")
                : before.startDate();
        LocalDate endAfter = mutable.containsKey("end_date")
                ? (LocalDate) mutable.get("end_date")
                : before.endDate();

        if (startAfter != null && endAfter != null && endAfter.isBefore(startAfter)) {
            throw new IllegalArgumentException(
                    "endDate " + endAfter + " cannot be before startDate " + startAfter);
        }

        boolean reopening = before.endDate() != null && endAfter == null;
        if (reopening) {
            tenantRepository.findActiveByTenancyAndPerson(before.tenancyId(), before.personId())
                    .filter(other -> !Objects.equals(other.uuid(), uuid))
                    .ifPresent(other -> {
                        throw new IllegalStateException(
                                "Cannot clear the end date on tenant " + uuid
                                        + ": that person is already active on this tenancy as "
                                        + other.uuid());
                    });
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

    /**
     * Removing the row, for a tenant added to the wrong tenancy. Someone who
     * genuinely left gets an end_date through patch instead -- a soft delete
     * takes the stay out of the record, and a stay that happened should stay in.
     */
    @Transactional
    public boolean softDelete(UUID uuid) {
        return tenantRepository.findById(uuid).map(tenant -> {
            if (!tenantRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("tenant", uuid, AuditMapper.toMap(tenant));
            return true;
        }).orElse(false);
    }
}
