package io.github.lordship.persons.internal;


import io.github.lordship.IntegrationTest;
import io.github.lordship.TestAuthSupport;
import io.github.lordship.access.Agent;
import io.github.lordship.access.AgentService;
import io.github.lordship.persons.Person;
import io.github.lordship.persons.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class PersonControllerIT extends IntegrationTest {

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AgentService agentService;

    @Autowired
    PersonService personService;

    @Test
    public void shouldNotModifyOmittedFields_whenEditingPerson() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        Optional<Agent> rootAgent = agentService.findByWorkEmail(rootEmail);
        assertTrue(rootAgent.isPresent());
        Optional<Person> personOptional = personService.findByID(rootAgent.get().personId());
        assertTrue(personOptional.isPresent());
        Person person = personOptional.get();
        String originalNameLast = person.nameLast();
        UUID personUuid = person.uuid();

        // only sending nameFirst - nameLast is OMITTED, should stay untouched
        String requestBody = """
                {
                    "nameFirst": "Erich"
                }
                """;

        // Act
        mockMvc.perform(patch("/persons/{uuid}", personUuid)
                .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameFirst").value("Erich"))
                .andExpect(jsonPath("$.nameLast").value(originalNameLast));
           // also check DB:
        Person personUpdated = personService.findByID(person.uuid()).orElseThrow();
        assertEquals("Erich", personUpdated.nameFirst());
        assertEquals(originalNameLast, personUpdated.nameLast());
    }

    @Test
    public void shouldClearField_whenExplicitNullIsProvided() throws Exception {
        // Arrange
        String token = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        Optional<Agent> rootAgent = agentService.findByWorkEmail(rootEmail);
        assertTrue(rootAgent.isPresent());
        Optional<Person> personOptional = personService.findByID(rootAgent.get().personId());
        assertTrue(personOptional.isPresent());
        Person person = personOptional.get();

          // Step 1: set birthday
        String setBirthdayBody = """
            {
                "birthday": "1990-01-01"
            }
            """;

        mockMvc.perform(patch("/persons/{uuid}", person.uuid())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBirthdayBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthday").value("1990-01-01"));

          // --- Step 2: now explicitly null it out
        String clearBirthdayBody = """
            {
                "birthday": null
            }
            """;

        // Act
        mockMvc.perform(patch("/persons/{uuid}", person.uuid())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clearBirthdayBody))
        // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthday").doesNotExist());

          // --- Step 3: verify against the actual DB-backed service too, not just the response DTO
        Person updated = personService.findByID(person.uuid()).orElseThrow();
        assertNull(updated.birthday());
    }
}
