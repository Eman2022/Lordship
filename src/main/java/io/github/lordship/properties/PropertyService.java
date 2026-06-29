package io.github.lordship.properties;

import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.properties.internal.PropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.time.LocalDate;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Transactional
    public Property createProperty(String propertyName, String propertyAddress) {
        PropertyRow row = new PropertyRow(
                propertyName,
                propertyAddress
        );
        return propertyRepository.save(row).toProperty();
    }

    public Optional<Property> findByPropertyCode(String propertyCode) {
        return propertyRepository.getPropertyOptional(propertyCode).map(PropertyRow::toProperty);
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

        return propertyRepository.patch(uuid, changes).map(PropertyRow::toProperty);
    }

    @Transactional
    public boolean deleteProperty(UUID uuid) {
        return propertyRepository.softDelete(uuid);
    }
}

