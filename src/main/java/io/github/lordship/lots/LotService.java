package io.github.lordship.lots;

import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.internal.LotRepository;
import io.github.lordship.lots.internal.LotRow;
import io.github.lordship.lots.internal.LotTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final LotTypeRepository lotTypeRepository;
    private final AuditService auditService;

    public LotService(LotRepository lotRepository,
                      LotTypeRepository lotTypeRepository,
                      AuditService auditService) {
        this.lotRepository = lotRepository;
        this.lotTypeRepository = lotTypeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Lot createLot(LotCreationRequest request) {
        LotRow saved = lotRepository.save(new LotRow(
                request.propertyId(),
                request.lotNumber(),
                request.lotTypeCode(),
                request.description(),
                request.notes(),
                request.sortOrder()
        ));

        auditService.recordInsert("lot", saved.uuid(), snapshot(saved));
        return saved.toLot();
    }

    @Transactional
    public Optional<Lot> updateLot(UUID uuid, LotUpdateRequest request) {
        return lotRepository.findById(uuid).map(existing -> {
            LotRow updated = lotRepository.update(new LotRow(
                    existing.uuid(),
                    existing.propertyId(),
                    request.lotNumber(),
                    request.lotTypeCode(),
                    request.description(),
                    request.notes(),
                    request.sortOrder(),
                    existing.createdAt(),
                    existing.deletedAt()
            ));

            // Captures renames and every other mutable-field change.
            auditService.recordUpdate("lot", uuid, snapshot(existing), snapshot(updated));
            return updated.toLot();
        });
    }

    // Soft delete recorded as an UPDATE (sets deleted_at). AuditService exposes
    // no public delete hook, and a soft delete is a state change, not a removal.
    @Transactional
    public boolean deleteLot(UUID uuid) {
        return lotRepository.findById(uuid).map(existing -> {
            lotRepository.softDelete(uuid);
            Map<String, Object> after = snapshot(existing);
            after.put("deleted_at", "now");
            auditService.recordUpdate("lot", uuid, snapshot(existing), after);
            return true;
        }).orElse(false);
    }

    public Optional<Lot> findById(UUID uuid) {
        return lotRepository.findById(uuid).map(LotRow::toLot);
    }

    public List<Lot> findByProperty(String propertyCode) {
        return lotRepository.findByProperty(propertyCode).stream().map(LotRow::toLot).toList();
    }

    public List<Lot> findDuplicateNumbers(String propertyCode) {
        return lotRepository.findDuplicateNumbers(propertyCode).stream().map(LotRow::toLot).toList();
    }

    public List<LotType> findActiveLotTypes() {
        return lotTypeRepository.findAllActive().stream()
                .map(io.github.lordship.lots.internal.LotTypeRow::toLotType)
                .toList();
    }

    private static Map<String, Object> snapshot(LotRow row) {
        Map<String, Object> map = new HashMap<>();
        map.put("property_code", row.propertyId());
        map.put("lot_number", row.lotNumber());
        map.put("lot_type_code", row.lotTypeCode());
        map.put("description", row.description());
        map.put("notes", row.notes());
        map.put("sort_order", row.sortOrder());
        return map;
    }
}
