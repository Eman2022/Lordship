package io.github.lordship.lots;

import io.github.lordship.TestAuthSupport;
import io.github.lordship.shared.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LotTests {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    EncryptionService encryptionService;

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

    private UUID insertTestLot(UUID propertyId, String lotNumber) {
        return jdbc.sql("""
                INSERT INTO lot (property_id, lot_number, lot_type_code, description, notes, sort_order)
                VALUES (:propertyId, :lotNumber, 'REN', 'Rental lot', :notes, 1)
                RETURNING uuid
                """)
                .param("propertyId", propertyId)
                .param("lotNumber", lotNumber)
                .param("notes", encryptionService.encrypt("Front row"))
                .query(UUID.class)
                .single();
    }

    @Test
    void activeLotIsNotSoftDeleted() {
        Lot lot = new Lot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12",
                "REN",
                "Rental lot",
                "Front row",
                1,
                LocalDateTime.now(),
                null
        );

        assertFalse(lot.isSoftDeleted());
    }

    @Test
    void deletedLotIsSoftDeleted() {
        Lot lot = new Lot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12",
                "REN",
                "Rental lot",
                "Front row",
                1,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        assertTrue(lot.isSoftDeleted());
    }

    @Test
    void unauthorizedCreateReturns403() throws Exception {
        LotCreationRequest request = new LotCreationRequest(UUID.randomUUID(), "12", "REN", null, null, 1);

        mockMvc.perform(post("/lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorizedCreateReturns201() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L001");

        LotCreationRequest request = new LotCreationRequest(
                propertyId,
                "12",
                "REN",
                "Rental lot",
                "Front row",
                1
        );

        mockMvc.perform(post("/lots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$.lotNumber").value("12"))
                .andExpect(jsonPath("$.lotTypeCode").value("REN"))
                .andExpect(jsonPath("$.description").value("Rental lot"))
                .andExpect(jsonPath("$.notes").value("Front row"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        String storedNotes = jdbc.sql("""
                SELECT notes FROM lot
                WHERE property_id = :propertyId AND lot_number = '12'
                """)
                .param("propertyId", propertyId)
                .query(String.class)
                .single();

        assertNotEquals("Front row", storedNotes);
        assertEquals("Front row", encryptionService.decrypt(storedNotes));
    }

    @Test
    void authorizedListByPropertyReturns200() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L002");
        insertTestLot(propertyId, "7");

        mockMvc.perform(get("/lots")
                        .param("property", "L002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").exists())
                .andExpect(jsonPath("$[0].propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$[0].lotNumber").value("7"))
                .andExpect(jsonPath("$[0].lotTypeCode").value("REN"));
    }

    @Test
    void authorizedDeleteReturns204() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L003");
        UUID lotId = insertTestLot(propertyId, "9");

        mockMvc.perform(delete("/lots/" + lotId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/lots/" + lotId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
