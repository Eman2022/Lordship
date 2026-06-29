package io.github.lordship.properties;

import io.github.lordship.properties.internal.PropertyCreateRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Nested
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PropertyServiceTest {

    @Autowired
    private PropertyService propertyService;

    private PropertyCreateRequest buildRequest() {
        return new PropertyCreateRequest(
                "Test Mobile Park",
                "999 Test Ave"
        );
    }

    @Test
    void createPropertyReturnsPropertyWithGeneratedFields() {
        Property created = propertyService.createProperty("Test Mobile Park", "999 Test Ave");

        assertNotNull(created.uuid());
        assertNotNull(created.createdAt());
        assertEquals("Test Mobile Park", created.propertyName());
        assertEquals("999 Test Ave", created.propertyAddress());
    }


@Test
void findByPropertyCodeReturnsCreatedProperty() {
    Property created = propertyService.createProperty("Test Mobile Park", "999 Test Ave");

    Optional<Property> found = propertyService.findByPropertyId(created.uuid());

    assertTrue(found.isPresent());
    assertEquals(created.uuid(), found.get().uuid());
}

@Test
void findByPropertyCodeReturnsEmptyForUnknownCode() {
    Optional<Property> found = propertyService.findByPropertyCode("NOPE99");

    assertTrue(found.isEmpty());
}

@Test
void findAllIncludesCreatedProperty() {
    Property created = propertyService.createProperty("Test Mobile Park","999 Test Ave");

    List<Property> all = propertyService.findAll();

    assertTrue(all.stream().anyMatch(p -> p.uuid().equals(created.uuid())));
}

}
