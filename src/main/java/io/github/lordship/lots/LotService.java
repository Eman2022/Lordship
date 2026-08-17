package io.github.lordship.lots;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.lots.internal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final LotPermissibleAgreementTypeRepository lotPermissibleAgreementTypeRepository;
    private final AuditService auditService;

    public LotService(LotRepository lotRepository,
                      LotPermissibleAgreementTypeRepository lotPermissibleAgreementTypeRepository,
                      AuditService auditService) {
        this.lotRepository = lotRepository;
        this.lotPermissibleAgreementTypeRepository = lotPermissibleAgreementTypeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Lot createLot(LotCreationRequest request) {
        LotRow saved = lotRepository.save(request.propertyId(), request.lotNumber());
        auditService.recordInsert("lot", saved.uuid(), AuditMapper.toMap(saved));
        return saved.toLot(List.of());
    }

    @Transactional
    public Optional<Lot> patchLot(UUID uuid, Map<String, Object> changes) {
        Optional<LotRow> beforeOpt = lotRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        LotRow before = beforeOpt.get();

        boolean rentableAfter = changes.containsKey("is_rentable")
                ? Boolean.TRUE.equals(changes.get("is_rentable"))
                : Boolean.TRUE.equals(before.isRentable());

        if (rentableAfter) {
            // A rentable lot cannot carry a reason -- clear whatever is there.
            if (before.notRentableReason() != null || changes.containsKey("not_rentable_reason")) {
                changes.put("not_rentable_reason", null);
            }
        } else {
            Object supplied = changes.containsKey("not_rentable_reason")
                    ? changes.get("not_rentable_reason")
                    : before.notRentableReason();
            String reasonAfter = (supplied == null) ? null : supplied.toString();
            if (reasonAfter == null || reasonAfter.isBlank()) {
                throw new IllegalArgumentException("notRentableReason is required when a lot is not rentable");
            }
            changes.put("not_rentable_reason", reasonAfter);
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

        List<LotPermissibleAgreementTypeRow> agreementTypes =
                lotPermissibleAgreementTypeRepository.findByLotId(uuid);
        return Optional.of(after.toLot(agreementTypes));
    }

    @Transactional
    public boolean deleteLot(UUID uuid) {
        return lotRepository.findById(uuid).map(existing -> {
            if (!lotRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("lot", uuid, AuditMapper.toMap(existing));
            return true;
        }).orElse(false);
    }

    public Optional<Lot> findById(UUID uuid) {
        return lotRepository.findById(uuid).map(row ->
                row.toLot(lotPermissibleAgreementTypeRepository.findByLotId(row.uuid())));
    }

    public List<Lot> findByProperty(String propertyCode) {
        return toLots(lotRepository.findByProperty(propertyCode));
    }

    public List<Lot> findDuplicateNumbers(String propertyCode) {
        return toLots(lotRepository.findDuplicateNumbers(propertyCode));
    }

    private List<Lot> toLots(List<LotRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> lotIds = rows.stream().map(LotRow::uuid).toList();
        Map<UUID, List<LotPermissibleAgreementTypeRow>> agreementTypesByLot =
                lotPermissibleAgreementTypeRepository.findByLotIds(lotIds).stream()
                        .collect(Collectors.groupingBy(LotPermissibleAgreementTypeRow::lotId));

        return rows.stream()
                .map(row -> row.toLot(agreementTypesByLot.getOrDefault(row.uuid(), List.of())))
                .toList();
    }
}