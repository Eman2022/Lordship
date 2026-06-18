package io.github.lordship.properties;

import io.github.lordship.access.internal.PropertyRow;
import io.github.lordship.properties.internal.PropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public List<Property> findAll() {
        return propertyRepository.findAll().stream()
                .map(PropertyRow::toProperty)
                .toList();
    }
}

