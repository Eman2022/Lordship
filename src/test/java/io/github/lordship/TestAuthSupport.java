package io.github.lordship;

import io.github.lordship.access.AgentLoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



public final class TestAuthSupport {

    private TestAuthSupport() {}

    // helper method
    public static String loginAsRoot(MockMvc mockMvc, ObjectMapper objectMapper,
                              String rootEmail, String rootPassword) throws Exception {

        AgentLoginRequest agentLoginRequest = new AgentLoginRequest(rootEmail, rootPassword);

        MvcResult mvcResult = mockMvc.perform(post("/api/agents/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workEmail").value(rootEmail))
                .andExpect(jsonPath("$.nameFull").value("Root Admin"))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        // --- Step 2: get token to use to authenticate registering another user
        String responseBody = mvcResult.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asString();
    }

    public static String loginAsAgent(MockMvc mockMvc, ObjectMapper objectMapper, String email, String password) throws Exception {

        AgentLoginRequest agentLoginRequest = new AgentLoginRequest(email, password);

        MvcResult mvcResult = mockMvc.perform(post("/api/agents/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(agentLoginRequest)))
            .andExpect(jsonPath("$.uuid").exists())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asString();
    }

}
