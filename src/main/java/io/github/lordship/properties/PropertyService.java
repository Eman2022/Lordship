package io.github.lordship.properties;

import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.properties.internal.PropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDate;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public PropertyService(PropertyRepository propertyRepository, AuditService auditService) {
        this.propertyRepository = propertyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Property createProperty(String propertyName, String propertyAddress) {
        String propertyCode = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 5)
                .toUpperCase();
        PropertyRow row = new PropertyRow(
                null,
                propertyCode,
                propertyName,
                propertyAddress,
                null, null, null, null);

        PropertyRow saved = propertyRepository.save(row);
        auditService.recordInsert("property", saved.uuid(), AuditMapper.toMap(saved));
        return saved.toProperty();
    }


    public Optional<Property> findByPropertyId(UUID propertyCode) {
        return propertyRepository.getPropertyOptional(propertyCode).map(PropertyRow::toProperty);
    }

    public List<Property> findAll() {
        return propertyRepository.findAll().stream()
                .map(PropertyRow::toProperty)
                .toList();
    }

    @Transactional
    public Optional<Property> patchProperty(UUID uuid, Map<String, Object> changes) {

        if (changes.containsKey("purchaseDate")) {
            Object pd = changes.get("purchaseDate");
            if (pd instanceof String s && !s.isBlank()) {
                changes.put("purchase_date", LocalDate.parse(s));
            } else {
                changes.put("purchase_date", null);
            }
            changes.remove("purchaseDate");
        }

        if (changes.containsKey("propertyManager")) {
            Object pm = changes.get("propertyManager");
            if (pm instanceof String s && !s.isBlank()) {
                changes.put("property_manager", UUID.fromString(s));
            } else {
                changes.put("property_manager", null);
            }
            changes.remove("propertyManager");
        }

        PropertyRow before = propertyRepository.getPropertyOptional(uuid).orElse(null);
        Optional<PropertyRow> result = propertyRepository.patch(uuid, changes);
        result.ifPresent(after -> {
            if (before != null) {
                AuditMapper.Diff diff = AuditMapper.diff(before, after);
                if (!diff.before().isEmpty()) {
                    auditService.recordUpdate("property", uuid, diff.before(), diff.after());
                }
            }
        });
        return result.map(PropertyRow::toProperty);
    }

    @Transactional
    public boolean deleteProperty(UUID uuid) {
        PropertyRow before = propertyRepository.getPropertyOptional(uuid).orElse(null);
        boolean deleted = propertyRepository.softDelete(uuid);
        if (deleted && before != null) {
            auditService.recordDelete("property", uuid, AuditMapper.toMap(before));
        }
        return deleted;
    }
}