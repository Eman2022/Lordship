package io.github.lordship.lots.internal;

import io.github.lordship.lots.Lot;
import io.github.lordship.lots.LotType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LotInternalTests {

    @Autowired
    LotRepository lotRepository;

    @Autowired
    LotTypeRepository lotTypeRepository;

    @Autowired
    JdbcClient jdbc;

    private UUID insertTestProperty(String propertyCode) {
        return jdbc.sql("""
                INSERT INTO property (property_code, property_name, property_address)
                VALUES (:propertyCode, 'Test Mobile Park', '999 Test Ave')
                RETURNING uuid
                """)
                .param("propertyCode", propertyCode)
                .query(UUID.class)
                .single();
    }

    private LotRow buildRow(UUID propertyId) {
        return new LotRow(
                propertyId,
                "12",
                "REN",
                "Rental lot",
                "Front row",
                1
        );
    }

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {
        UUID propertyId = insertTestProperty("I001");

        LotRow saved = lotRepository.save(buildRow(propertyId));

        assertNotNull(saved.uuid());
        assertEquals(propertyId, saved.propertyId());
        assertEquals("12", saved.lotNumber());
        assertEquals("REN", saved.lotTypeCode());
        assertNotNull(saved.createdAt());
        assertNull(saved.deletedAt());
    }

    @Test
    void findALotById() {
        UUID propertyId = insertTestProperty("I002");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        Optional<LotRow> found = lotRepository.findById(saved.uuid());

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void updateChangesMutableLotFields() {
        UUID propertyId = insertTestProperty("I003");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        LotRow updated = lotRepository.update(new LotRow(
                saved.uuid(),
                saved.propertyId(),
                "14",
                "VAL",
                "Vacant lot",
                "Ready for assignment",
                3,
                saved.createdAt(),
                saved.deletedAt()
        ));

        assertEquals(saved.uuid(), updated.uuid());
        assertEquals("14", updated.lotNumber());
        assertEquals("VAL", updated.lotTypeCode());
        assertEquals("Vacant lot", updated.description());
        assertEquals("Ready for assignment", updated.notes());
        assertEquals(3, updated.sortOrder());
    }

    @Test
    void softDeleteRemovesFromFindById() {
        UUID propertyId = insertTestProperty("I004");
        LotRow saved = lotRepository.save(buildRow(propertyId));

        lotRepository.softDelete(saved.uuid());

        Optional<LotRow> found = lotRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllActiveLotTypesReturnsSeededLookupValues() {
        List<LotTypeRow> lotTypes = lotTypeRepository.findAllActive();

        assertFalse(lotTypes.isEmpty());
        assertTrue(lotTypes.stream().allMatch(LotTypeRow::active));
    }

    @Test
    void lotTypeRowMapsToPublicLotType() {
        LotTypeRow row = new LotTypeRow("REN", "Rental", "Rental lot type", true, 2);

        LotType lotType = row.toLotType();

        assertEquals("REN", lotType.code());
        assertEquals("Rental", lotType.label());
        assertEquals("Rental lot type", lotType.description());
        assertTrue(lotType.active());
        assertEquals(2, lotType.sortOrder());
    }

    @Test
    void lotResponseMapsPublicLot() {
        UUID lotId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        Lot lot = new Lot(
                lotId,
                propertyId,
                "12",
                "REN",
                "Rental lot",
                "Front row",
                1,
                LocalDateTime.now(),
                null
        );

        LotResponse response = LotResponse.from(lot);

        assertEquals(lotId, response.uuid());
        assertEquals(propertyId, response.propertyId());
        assertEquals("12", response.lotNumber());
        assertEquals("REN", response.lotTypeCode());
        assertEquals("Rental lot", response.description());
        assertEquals("Front row", response.notes());
        assertEquals(1, response.sortOrder());
    }
}
