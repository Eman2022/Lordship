package io.github.lordship.meters.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.access.AgentLoginRequest;
import io.github.lordship.meters.MeterMeasurement;
import io.github.lordship.meters.MeterType;
import io.github.lordship.properties.internal.PropertyRow;
import io.micrometer.core.instrument.Meter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Transactional
public class MeterControllerIT extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MeterRepository meterRepository;

    @Autowired
    JdbcClient jdbc;

    private MeterRow buildRow(UUID meterId) {
        return MeterRow.forInsert(
                meterId,
                0.0,
                0.0,
                MeterType.WATER,
                MeterMeasurement.GALLONS,
                true
        );
    }

    private UUID insertTestProperty() {
        PropertyRow propertyRow = jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES ('TST01', 'Test Mobile Park', '999 Test Ave') RETURNING *
                """).query(PropertyRow.class)
                .single();
        return propertyRow.uuid();
    }

    private UUID insertTestLot(UUID propertyId) {
        return jdbc.sql("""
                INSERT INTO lot (property_id, lot_number, target_rent)
                VALUES (:propertyId, '1', 350.0)
                RETURNING uuid
                """)
                .param("propertyId", propertyId)
                .query(UUID.class)
                .single();
    }

    private UUID setupFullChain() {
        UUID propertyId = insertTestProperty();
        return insertTestLot(propertyId);
    }

    private String loginAsRoot() throws Exception {
        AgentLoginRequest loginRequest = new AgentLoginRequest(rootEmail, rootPassword);

        MvcResult result = mockMvc.perform(post("/agents/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    // REPOSITORY TESTS
    @Test
    void findAMeterById() {
        UUID meterId = setupFullChain();
        MeterRow saved = meterRepository.save(buildRow(meterId));

        Optional<MeterRow> found = meterRepository.findById(saved.uuid());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void softDeleteRemovesFromTable() {
        UUID meterId = setupFullChain();
        MeterRow saved = meterRepository.save(buildRow(meterId));

        meterRepository.softDelete(saved.uuid());

        assertTrue(meterRepository.findById(saved.uuid()).isEmpty());
        assertTrue(meterRepository.findMeterByLot(saved.meterId()).isEmpty());
    }

    @Test
    void patchUpdatesAllowedFields() {
        UUID meterId = setupFullChain();
        MeterRow saved = meterRepository.save(buildRow(meterId));

        Map<String, Object> mutable = Map.of(
                "title", "Updated Title"
        );

        Optional<MeterRow> patched = meterRepository.patch(saved.uuid(), mutable);

        assertTrue(patched.isPresent());
        assertEquals("Updated Title", patched.get().title());
    }
}