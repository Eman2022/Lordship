package io.github.lordship.tenancy;


import io.github.lordship.accounts.AccountService;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.shared.EncryptionService;
import io.github.lordship.tenancy.internal.TenancyCreateRequest;
import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cglib.core.Local;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class TenancyService {
    private final TenancyRepository tenancyRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;
    private final AccountService accountService;

    private static final Logger log = LoggerFactory.getLogger(TenancyService.class);

    public TenancyService(
            TenancyRepository tenancyRepository,
            EncryptionService encryptionService,
            AuditService auditService,
            AccountService accountService
    ) {
        this.tenancyRepository = tenancyRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
        this.accountService = accountService;
    }

    // Forces a maximum of two tenancies for a lot
    @Transactional
    public Tenancy create(TenancyCreateRequest request) {
        List<TenancyRow> active = tenancyRepository.findActiveByLot(request.lotId());

        if (active.size() >= 2) {
            throw new IllegalStateException("Lot cannot have more than two tenancies at a time");
        }

        TenancyRow row = tenancyRepository.save(
                TenancyRow.forInsert(
                        request.lotId(),
                        LocalDate.now(),
                        null
                )
        );

        auditService.recordInsert("tenancy", row.uuid(), AuditMapper.toMap(row));

        Tenancy tenancy = row.toTenancy();
        accountService.createAccount(tenancy.uuid(), null);
        log.info("Account auto-created for tenancy uuid={}", tenancy.uuid());
        return tenancy;
    }

    // Manages timeslots for the second tenancy
    @Transactional
    public void enforceSecondTenancyLimit(UUID lotId) {
        List<TenancyRow> active = tenancyRepository.findActiveByLot(lotId);

        TenancyRow second = active.stream()
                .max((a, b) -> a.startDate().compareTo(b.startDate()))
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

    @Transactional
    public Tenancy endTenancy(UUID uuid, LocalDate endDate) {
        TenancyRow before = tenancyRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Tenancy not found: " + uuid));

        if (before.endDate() != null) {
            throw new IllegalStateException("Tenancy already closed: " + uuid);
        }

        TenancyRow after = tenancyRepository.close(uuid, endDate);

        var diff = AuditMapper.diff(before, after);
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("tenancy", uuid, diff.before(), diff.after());
        }

        return after.toTenancy();
    }

    // mutable hashmap to be able to properly patch changes
    @Transactional
    public Optional<Tenancy> patchTenancy(UUID uuid, Map<String, Object> changes) {
        Map<String, Object> mutable = new HashMap<>(changes);
        if (mutable.containsKey("start_date")) {
            Object startDate = mutable.get("start_date");
            if (startDate instanceof String s && !s.isBlank()) {
                mutable.put("start_date", LocalDate.parse(s));
            } else {
                mutable.put("start_date", null);
            }
        }

        if (mutable.containsKey("end_date")) {
            Object endDate = mutable.get("end_date");
            if (endDate instanceof String s && !s.isBlank()) {
                mutable.put("end_date", LocalDate.parse(s));
            } else {
                mutable.put("end_date", null);
            }
        }

        if (mutable.containsKey("lot_id")) {
            Object lotId = mutable.get("lot_id");
            if (lotId instanceof String s && !s.isBlank()) {
                mutable.put("lot_id", UUID.fromString(s));
            } else {
                mutable.put("lot_id", null);
            }
        }

        Optional<TenancyRow> previousTenancy = tenancyRepository.findById(uuid);
        if (previousTenancy.isEmpty()) {
            return Optional.empty();
        }

        TenancyRow before = previousTenancy.get();

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
            tenancyRepository.softDelete(uuid);
            auditService.recordDelete("tenancy", uuid, AuditMapper.toMap(tenancy));
            return true;
        }).orElse(false);
    }
}
