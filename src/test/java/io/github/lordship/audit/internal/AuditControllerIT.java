package io.github.lordship.audit.internal;

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
import static org.hamcrest.Matchers.nullValue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class AuditControllerIT extends IntegrationTest {

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void shouldReturnMaskedAuditLog_WhenNewAgentChangesPersonInfo() throws Exception {
        // Arrange
        String rootAuthToken = TestAuthSupport.loginAsRoot(mockMvc, objectMapper, rootEmail, rootPassword);
        String registerAgentBody = """
            {
                "nameFirst": "Jimmy",
                "nameLast": "John",
                "workEmail": "JimmyJohn@lordship.com",
                "password": "jx341004rtrtrt"
            }
            """;

        MvcResult result1 = mockMvc.perform(post("/agents/register")
                .header("Authorization", "Bearer " + rootAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerAgentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workEmail").value("JimmyJohn@lordship.com"))
                .andExpect(jsonPath("$.fullName").value("Jimmy John"))
                .andExpect(jsonPath("$.uuid").exists())
                .andReturn();

        String jimmyUuid = JsonPath.read(result1.getResponse().getContentAsString(), "$.uuid");
        assertFalse(jimmyUuid.isEmpty());
        String grantRoleForJimmy = String.format("""
                {
                    "agentId" : "%s",
                    "roleName" : "Property Manager"
                }
                """, jimmyUuid);

        mockMvc.perform(post("/roles/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + rootAuthToken)
                        .content(grantRoleForJimmy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.agentId").exists())
                .andExpect(jsonPath("$.roleId").exists())
                .andExpect(jsonPath("$.grantedBy").exists());

        String jimmyLogin = """
                {
                    "workEmail" : "JimmyJohn@lordship.com",
                    "password" : "jx341004rtrtrt"
                }
                """;

        MvcResult result3 = mockMvc.perform(post("/agents/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jimmyLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workEmail").value("JimmyJohn@lordship.com"))
                .andExpect(jsonPath("$.fullName").value("Jimmy John"))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String jimmyUUID = JsonPath.read(result3.getResponse().getContentAsString(), "$.uuid");
        String jimmyToken = JsonPath.read(result3.getResponse().getContentAsString(), "$.token");
        assertFalse(jimmyToken.isEmpty());

        String jimmyCreatePerson = """
                {
                    "nameFirst" : "Linda",
                    "nameLast" : "Belcher"
                }
                """;

        MvcResult result4 = mockMvc.perform(post("/persons/create")
                        .header("Authorization", "Bearer " + jimmyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(jimmyCreatePerson))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.nameFirst").value("Linda"))
                .andExpect(jsonPath("$.nameLast").value("Belcher"))
                .andReturn();

        UUID lindaPersonId = UUID.fromString(JsonPath.read(result4.getResponse().getContentAsString(), "$.uuid"));

        String jimmyPatchesLinda1 =
                """
                    {
                        "birthday" : "1990-05-12"
                    }
               """;
        String jimmyPatchesLinda2 =
                """
                    {
                        "social" : "547-25-6371"
                    }
               """;
        String jimmyPatchesLinda3 =
                """
                    {
                        "personalEmail" : "lindaisAwesome@yahoo.com",
                        "personalPhone" : "360-211-4510"
                    }
               """;
        String jimmyPatchesLinda4 =
                """
                    {
                        "personalPhone" : null
                    }
               """;

        // Act
        MvcResult patch1Result = mockMvc.perform(patch("/persons/" + lindaPersonId)
                        .header("Authorization", "Bearer " + jimmyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jimmyPatchesLinda1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(lindaPersonId.toString()))
                .andExpect(jsonPath("$.birthday").value("1990-05-12"))
                .andReturn();

        MvcResult patch2Result = mockMvc.perform(patch("/persons/" + lindaPersonId)
                        .header("Authorization", "Bearer " + jimmyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jimmyPatchesLinda2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(lindaPersonId.toString()))
                .andExpect(jsonPath("$.social").value("***-**-6371"))
                .andReturn();

        MvcResult patch3Result = mockMvc.perform(patch("/persons/" + lindaPersonId)
                        .header("Authorization", "Bearer " + jimmyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jimmyPatchesLinda3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(lindaPersonId.toString()))
                .andExpect(jsonPath("$.personalEmail").value("lindaisAwesome@yahoo.com"))
                .andExpect(jsonPath("$.personalPhone").value("360-211-4510"))
                .andReturn();

        MvcResult patch4Result = mockMvc.perform(patch("/persons/" + lindaPersonId)
                        .header("Authorization", "Bearer " + jimmyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jimmyPatchesLinda4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(lindaPersonId.toString()))
                .andExpect(jsonPath("$.personalPhone").value(nullValue()))
                .andReturn();

        String getJimmyLogs = """
                {
                    "page": 0,
                    "pageSize": 10,
                    "sortBy": "changed_at",
                    "ascending" : true
                }
                """;

        MvcResult getLogsResult = mockMvc.perform(get("/auditlogs/byagent/" + jimmyUUID)
                        .header("Authorization", "Bearer " + jimmyToken)
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("sortBy", "changed_at")
                        .param("ascending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andReturn();

        //System.out.println(JsonPath.read(getLogsResult.getResponse().getContentAsString(), "$.content").toString());
    }
}
