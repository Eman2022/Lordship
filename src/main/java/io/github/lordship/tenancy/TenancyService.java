package io.github.lordship.tenancy;


import io.github.lordship.accounts.AccountService;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.Lot;
import io.github.lordship.lots.LotService;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class TenancyService {
    private final TenancyRepository tenancyRepository;
    private final AuditService auditService;
    private final AccountService accountService;
    private final LotService lotService;

    private static final Logger log = LoggerFactory.getLogger(TenancyService.class);

    public TenancyService(
            TenancyRepository tenancyRepository,
            AuditService auditService,
            AccountService accountService,
            LotService lotService
    ) {
        this.tenancyRepository = tenancyRepository;
        this.auditService = auditService;
        this.accountService = accountService;
        this.lotService = lotService;
    }

    /**
     * A lot admits a new tenancy only when it is rentable, and never more than
     * two at a time. Two is deliberate: an outgoing tenancy and its replacement
     * overlap while the first is being wound up, and the office cannot be made
     * to wait on that to set the next one up.
     *
     * <p>{@code is_rentable} governs new tenancies only. A lot that becomes
     * flooded, condemned or held for a road widening keeps the tenants already
     * on it -- same reasoning as revoking a permissible agreement type not
     * invalidating a charge term already signed and served.
     *
     * <p>{@code LotService.findById} filters soft-deleted lots, so this also
     * refuses a tenancy on a lot that was deleted.
     */
    @Transactional
    public Tenancy create(UUID lotId) {
        Lot lot = lotService.findById(lotId)
                .orElseThrow(() -> new EntityNotFoundException("Lot not found: " + lotId));

        if (Boolean.FALSE.equals(lot.isRentable())) {
            throw new IllegalStateException("Lot " + lot.lotNumber()
                    + " cannot take a new tenancy: " + lot.notRentableReason());
        }

        List<TenancyRow> active = tenancyRepository.findActiveByLot(lotId);

        if (active.size() >= 2) {
            throw new IllegalStateException("Lot cannot have more than two tenancies at a time");
        }

        TenancyRow row = tenancyRepository.save(lotId);
        Tenancy tenancy = row.toTenancy();
        accountService.createAccount(tenancy.uuid(), null);

        // log
        auditService.recordInsert("tenancy", row.uuid(), AuditMapper.toMap(row));
        return tenancy;
    }

    /**
     * Closes the newer of two overlapping tenancies once it has had its month.
     * Does nothing while a lot has fewer than two, and does nothing while any
     * active tenancy is missing its possession date: a tenancy with no
     * start_date cannot be ranked against the others, and closing the wrong one
     * is worse than closing neither.
     */
    @Transactional
    public void enforceSecondTenancyLimit(UUID lotId) {
        List<TenancyRow> active = tenancyRepository.findActiveByLot(lotId);

        if (active.size() < 2) {
            return;
        }
        if (active.stream().anyMatch(t -> t.startDate() == null)) {
            return;
        }

        TenancyRow second = active.stream()
                .max(Comparator.comparing(TenancyRow::startDate))
                .orElseThrow();

        LocalDate start = second.startDate();
        LocalDate now = LocalDate.now();

        // Set to one full month (date) instead of an amount of days
        if(!start.plusMonths(1).isAfter(now)) {
            tenancyRepository.close(second.uuid(), now);
            auditService.recordUpdate(
                    "tenancy",
                    second.uuid(),
                    Map.of("end_date", start),
                    Map.of("end_date", now)
            );
        }
    }


    public Optional<Tenancy> findTenancyById(UUID uuid) {
        return tenancyRepository.findById(uuid).map(TenancyRow::toTenancy);
    }

    public List<Tenancy> findActiveTenancyByLot(UUID lotId) {
        return tenancyRepository.findActiveByLot(lotId)
                .stream()
                .map(TenancyRow::toTenancy)
                .toList();
    }

    /**
     * The one door onto a tenancy's dates. There is no separate close endpoint:
     * ending a tenancy is setting its end_date, so it goes through here with
     * everything else.
     *
     * <p>end_date is a state transition, not just a column. Null to a date
     * closes the tenancy; date to a different date corrects a figure someone
     * typed wrong; a date back to null reopens it, which is refused when the
     * lot already carries its two active tenancies. Without that last check a
     * reopen is a third way onto a full lot, since the create path never sees
     * it.
     *
     * <p>Mutable hashmap so the no-op keys can be dropped before the write.
     */
    @Transactional
    public Optional<Tenancy> patchTenancy(UUID uuid, Map<String, Object> changes) {
        Optional<TenancyRow> beforeOpt = tenancyRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        TenancyRow before = beforeOpt.get();

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
            return Optional.of(before.toTenancy());
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
            long othersActive = tenancyRepository.findActiveByLot(before.lotId()).stream()
                    .filter(row -> !Objects.equals(row.uuid(), uuid))
                    .count();

            if (othersActive >= 2) {
                throw new IllegalStateException(
                        "Cannot reopen tenancy " + uuid + ": its lot already has two active tenancies");
            }
        }

        Optional<TenancyRow> updatedTenancy = tenancyRepository.patch(uuid, mutable);
        if (updatedTenancy.isEmpty()) {
            return Optional.empty();
        }
        TenancyRow after = updatedTenancy.get();

        var diff = AuditMapper.diff(before, after);
        if(!diff.before().isEmpty()) {
            auditService.recordUpdate("tenancy", uuid, diff.before(), diff.after());
        }

        return Optional.of(after.toTenancy());
    }

    @Transactional
    public boolean softDelete(UUID uuid) {
        return tenancyRepository.findById(uuid).map(tenancy -> {
            if (!tenancyRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("tenancy", uuid, AuditMapper.toMap(tenancy));
            return true;
        }).orElse(false);
    }
}