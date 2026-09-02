package io.github.lordship.tenants.internal;

import io.github.lordship.TestAuthSupport;
import io.github.lordship.lots.Lot;
import io.github.lordship.lots.LotService;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import io.github.lordship.properties.Property;
import io.github.lordship.properties.PropertyService;
import io.github.lordship.tenancy.TenancyService;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import io.github.lordship.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Transactional
public class TenantControllerIT extends IntegrationTest {
    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenancyService tenancyService;

    @Autowired
    LotService lotService;

    @Autowired
    PropertyService propertyService;

    @Autowired
    PersonService personService;

    private UUID setupFullChain() {
        Property property = propertyService.createProperty("Test Mobile Park", "999 Test Ave");
        Lot lot = lotService.createLot(property.uuid(), "1");
        return tenancyService.create((lot.uuid())).uuid();
    }

    private UUID setupPerson() {
        Person person = personService.createPersonFromName("Jack Lee");
        return person.uuid();
    }

    @Test
    void createTenant_unauthorized_returns403() throws Exception {
        var request = new TenantCreateRequest(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(
                        post("/api/tenants/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void createTenant_invalidPayload_returns400() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        var invalidJson = """
                    { "tenancyId": null, "personId": null }
                """;

        mockMvc.perform(
                        post("/api/tenants/create")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTenant_shouldReturn404_whenTenantDoesNotExist() throws Exception {
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);

        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTenant_shouldReturn403_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/tenants/{uuid}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
