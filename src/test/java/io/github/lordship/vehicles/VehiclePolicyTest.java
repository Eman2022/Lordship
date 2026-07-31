package io.github.lordship.vehicles;

import io.github.lordship.properties.internal.PropertyRow;
import io.github.lordship.vehicles.internal.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional

public class VehiclePolicyTest {

    @Autowired
    VehicleService vehicleService;

    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    JdbcClient jdbc;

    private UUID insertTestProperty() {
        PropertyRow row = jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave') RETURNING *
                """).query(PropertyRow.class).single();
        return row.uuid();
    }

    @Test
    void setPolicyCreatesNewPolicy() {
        UUID propertyCode = insertTestProperty();

        VehiclePolicy policy = vehicleService.setPolicy(propertyCode, 2, new BigDecimal("25.00"), "Standard");

        assertNotNull(policy.uuid());
        assertEquals(propertyCode, policy.propertyUuid());
        assertEquals(2, policy.freeVehicleLimit());
        assertEquals(new BigDecimal("25.00"), policy.extraVehicleFee());
    }

    @Test
    void getPolicyReturnsExistingPolicy() {
        UUID propertyCode = insertTestProperty();
        vehicleService.setPolicy(propertyCode, 2, new BigDecimal("25.00"), null);

        Optional<VehiclePolicy> found = vehicleService.getPolicy(propertyCode);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().freeVehicleLimit());
    }

    @Test
    void getPolicyReturnsEmptyWhenNoneSet() {
        UUID propertyCode = insertTestProperty();

        Optional<VehiclePolicy> found = vehicleService.getPolicy(propertyCode);

        assertTrue(found.isEmpty());
    }

    @Test
    void setPolicyOverwritesExistingPolicy() {
        UUID propertyCode = insertTestProperty();
        vehicleService.setPolicy(propertyCode, 2, new BigDecimal("25.00"), null);

        // Update with new values
        vehicleService.setPolicy(propertyCode, 3, new BigDecimal("50.00"), "Updated");

        Optional<VehiclePolicy> found = vehicleService.getPolicy(propertyCode);
        assertTrue(found.isPresent());
        assertEquals(3, found.get().freeVehicleLimit());
        assertEquals(new BigDecimal("50.00"), found.get().extraVehicleFee());
    }
}
