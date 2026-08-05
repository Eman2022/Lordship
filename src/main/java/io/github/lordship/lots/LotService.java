package io.github.lordship.lots;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.internal.LotRepository;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.lots.internal.LotUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final AuditService auditService;

    public LotService(LotRepository lotRepository,
                      AuditService auditService) {
        this.lotRepository = lotRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Lot createLot(LotCreationRequest request) {
        LotRow saved = lotRepository.save(new LotRow(
                request.propertyId(),
                request.lotNumber(),
                request.description(),
                request.notes(),
                request.sortOrder()
        ));

        auditService.recordInsert("lot", saved.uuid(), AuditMapper.toMap(saved));
        return saved.toLot();
    }

    @Transactional
    public Optional<Lot> updateLot(UUID uuid, LotUpdateRequest request) {
        return lotRepository.findById(uuid).map(existing -> {
            LotRow updated = lotRepository.update(new LotRow(
                    existing.uuid(),
                    existing.propertyId(),
                    request.lotNumber(),
                    request.description(),
                    request.notes(),
                    request.sortOrder(),
                    existing.createdAt(),
                    existing.deletedAt()
            ));

            // Only the fields that actually changed are logged, so a rename shows up as
            // just lotNumber rather than the whole row.
            var diff = AuditMapper.diff(existing, updated);
            if (!diff.before().isEmpty()) {
                auditService.recordUpdate("lot", uuid, diff.before(), diff.after());
            }
            return updated.toLot();
        });
    }

    // Partial update: only the fields present in `changes` are written. Keys are
    // already snake_case column names mapped by the controller.
    @Transactional
    public Optional<Lot> patchLot(UUID uuid, Map<String, Object> changes) {
        Optional<LotRow> beforeOpt = lotRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        LotRow before = beforeOpt.get();

        if (changes.containsKey("sort_order")) {
            Object so = changes.get("sort_order");
            if (so instanceof String s && !s.isBlank()) {
                changes.put("sort_order", Integer.parseInt(s));
            } else if (so instanceof String) {
                changes.put("sort_order", null);
            }
        }

        Optional<LotRow> afterOpt = lotRepository.patch(uuid, changes);
        if (afterOpt.isEmpty()) {
            return Optional.empty();
        }
        LotRow after = afterOpt.get();

        var diff = AuditMapper.diff(before, after);
        if (!diff.before().isEmpty()) {
            auditService.recordUpdate("lot", uuid, diff.before(), diff.after());
        }
        return Optional.of(after.toLot());
    }

    @Transactional
    public boolean deleteLot(UUID uuid) {
        return lotRepository.findById(uuid).map(existing -> {
            lotRepository.softDelete(uuid);
            auditService.recordDelete("lot", uuid, AuditMapper.toMap(existing));
            return true;
        }).orElse(false);
    }

    public Optional<Lot> findById(UUID uuid) {
        return lotRepository.findById(uuid).map(LotRow::toLot);
    }

    public List<Lot> findByProperty(String propertyCode) {
        return lotRepository.findByProperty(propertyCode).stream()
                .map(LotRow::toLot)
                .toList();
    }

    public List<Lot> findDuplicateNumbers(String propertyCode) {
        return lotRepository.findDuplicateNumbers(propertyCode).stream()
                .map(LotRow::toLot)
                .toList();
    }
}
