package io.github.lordship.lots.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.TestAuthSupport;
import io.github.lordship.lots.LotCreationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class LotControllerIT extends IntegrationTest {

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
                INSERT INTO lot (property_id, lot_number, description, notes, sort_order)
                VALUES (:propertyId, :lotNumber, 'Rental lot', :notes, 1)
                RETURNING uuid
                """)
                .param("propertyId", propertyId)
                .param("lotNumber", lotNumber)
                .param("notes", "Front row")
                .query(UUID.class)
                .single();
    }

    @Test
    void createLot_shouldReturn403_whenUnauthorized() throws Exception {
        // Arrange
        LotCreationRequest request = new LotCreationRequest(UUID.randomUUID(), "12");

        // Act
        mockMvc.perform(post("/api/lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert
                .andExpect(status().isForbidden());
    }

    @Test
    void createLot_shouldReturn201_withMinimalFields() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L001");

        LotCreationRequest request = new LotCreationRequest(propertyId, "12");

        // Act
        mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert: only what was supplied (plus DB defaults) comes back -- everything
                // else is left for a follow-up PATCH, per the create-minimal design.
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$.lotNumber").value("12"))
                .andExpect(jsonPath("$.isRentable").value(true))
                .andExpect(jsonPath("$.notRentableReason").doesNotExist())
                .andExpect(jsonPath("$.lotAddress").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.sortOrder").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.deletedAt").doesNotExist())
                // The DB's default rectangle, since shape_data wasn't supplied at creation.
                .andExpect(jsonPath("$.shapeData.vertices.length()").value(4))
                .andExpect(jsonPath("$.permissibleAgreementTypes").isArray())
                .andExpect(jsonPath("$.permissibleAgreementTypes").isEmpty());
    }

    @Test
    void listByProperty_shouldReturn200_withLots() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L002");
        insertTestLot(propertyId, "7");

        // Act
        mockMvc.perform(get("/api/lots")
                        .param("property", "L002")
                        .header("Authorization", "Bearer " + token))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").exists())
                .andExpect(jsonPath("$[0].propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$[0].lotNumber").value("7"));
    }

    @Test
    void patchLot_shouldUpdateField_andReturn200() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L004");
        UUID lotId = insertTestLot(propertyId, "5");

        // Act
        mockMvc.perform(patch("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lotNumber": "99", "description": "Updated lot" }
                                """))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotNumber").value("99"))
                .andExpect(jsonPath("$.description").value("Updated lot"))
                .andExpect(jsonPath("$.notes").value("Front row"));
    }

    @Test
    void patchLot_shouldClearField_whenExplicitNullProvided() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L005");
        UUID lotId = insertTestLot(propertyId, "6");

        // Act
        mockMvc.perform(patch("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "notes": null }
                                """))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void patchLot_shouldReturn404_whenNotFound() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);

        // Act
        mockMvc.perform(patch("/api/lots/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lotNumber": "99" }
                                """))
                // Assert
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLot_shouldReturn204_andSubsequentGetReturns404() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        UUID propertyId = insertTestProperty("L003");
        UUID lotId = insertTestLot(propertyId, "9");

        // Act
        mockMvc.perform(delete("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + token))
                // Assert
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}