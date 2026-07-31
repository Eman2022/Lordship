package io.github.lordship.properties.internal;

import com.jayway.jsonpath.JsonPath;
import io.github.lordship.IntegrationTest;
import io.github.lordship.TestAuthSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class PropertyControllerIT extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String loginAsRoot() throws Exception {
        return TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
    }

    // ── Unauthorized tests ────────────────────────────────────

    @Test
    void createProperty_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(post("/properties/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "Test Mobile Park",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProperty_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/properties/TST01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllProperties_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/properties"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchProperty_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(patch("/properties/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyName": "Updated Name" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProperty_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(delete("/properties/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // ── Authorized tests ──────────────────────────────────────

    @Test
    void createProperty_shouldReturn201_withCorrectFields() throws Exception {
        String token = loginAsRoot();

        mockMvc.perform(post("/properties/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "Test Mobile Park",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.propertyCode").exists())
                .andExpect(jsonPath("$.propertyName").value("Test Mobile Park"))
                .andExpect(jsonPath("$.propertyAddress").value("999 Test Ave"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createProperty_shouldReturn400_whenPropertyNameIsBlank() throws Exception {
        String token = loginAsRoot();

        mockMvc.perform(post("/properties/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProperty_shouldReturn200_withCorrectFields() throws Exception {
        String token = loginAsRoot();

        // Create first
        MvcResult createResult = mockMvc.perform(post("/properties/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "Test Mobile Park",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String propertyCode = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.propertyCode");

        // Fetch by code
        mockMvc.perform(get("/properties/{propertyCode}", propertyCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyCode").value(propertyCode))
                .andExpect(jsonPath("$.propertyName").value("Test Mobile Park"));
    }

    @Test
    void getProperty_shouldReturn404_whenPropertyDoesNotExist() throws Exception {
        String token = loginAsRoot();

        mockMvc.perform(get("/properties/NOPE9")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllProperties_shouldReturn200_andIncludeCreatedProperty() throws Exception {
        String token = loginAsRoot();

        MvcResult createResult = mockMvc.perform(post("/properties/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "Test Mobile Park",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String propertyCode = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.propertyCode");

        mockMvc.perform(get("/properties/" + propertyCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.propertyCode == '" + propertyCode + "')]").exists());
    }

    @Test
    void patchProperty_shouldReturn200_andOnlyUpdateSentFields() throws Exception {
        String token = loginAsRoot();

        MvcResult createResult = mockMvc.perform(post("/properties/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "Test Mobile Park",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String propertyUuid = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.uuid");

        // Only patch the name — address should stay untouched
        mockMvc.perform(patch("/properties/{uuid}", propertyUuid)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyName": "Updated Park Name" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyName").value("Updated Park Name"))
                .andExpect(jsonPath("$.propertyAddress").value("999 Test Ave"));
    }

    @Test
    void patchProperty_shouldReturn404_whenPropertyDoesNotExist() throws Exception {
        String token = loginAsRoot();

        mockMvc.perform(patch("/properties/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyName": "Updated Park Name" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProperty_shouldReturn204_andSubsequentGetReturns404() throws Exception {
        String token = loginAsRoot();

        MvcResult createResult = mockMvc.perform(post("/properties/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "propertyName": "Test Mobile Park",
                                    "propertyAddress": "999 Test Ave"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String propertyUuid = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.uuid");

        String propertyCode = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.propertyCode");

        // Delete
        mockMvc.perform(delete("/properties/{uuid}", propertyUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Confirm gone
        mockMvc.perform(get("/properties/{propertyCode}", propertyCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
