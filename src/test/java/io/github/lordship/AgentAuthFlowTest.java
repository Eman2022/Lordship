package io.github.lordship;


import io.github.lordship.access.AgentRegistrationRequest;
import io.github.lordship.access.AgentLoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional //TODO: do more tests to confirm this annotation rolls back database changes.
public class AgentAuthFlowTest {

    @Value("${lordship.root.password}")
    private String rootPassword;

    @Value("${lordship.root.email}")
    private String rootEmail;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // helper method
    private String loginAsRoot() throws Exception {
        // --- Step 1: Log in as root user (root user will create agent accounts)
        AgentLoginRequest agentLoginRequest = new AgentLoginRequest(rootEmail, rootPassword);

        MvcResult mvcResult = mockMvc.perform(post("/agents/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workEmail").value(rootEmail))
                .andExpect(jsonPath("$.fullName").value("Root Admin"))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        // --- Step 2: get token to use to authenticate registering another user
        String responseBody = mvcResult.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asString();
    }


    @Test
    void rootAgentLogsInAndRegistersANewAgent() throws Exception {
        // an agent that needs a superuser to help them register
        String testAgentNameFirst = "Bilbo";
        String testAgentNameLast = "Baggins";
        String testAgentEmail = "baggins@lordship.com";
        String testAgentPass = "pass123456789";

        // steps 1 & 2: login and get token
        String rootUserToken = loginAsRoot();

        // --- Step 3: register another user
        AgentRegistrationRequest agentRegistrationRequest = new AgentRegistrationRequest(testAgentNameFirst, testAgentNameLast, null,
                testAgentEmail, null, testAgentPass);

        mockMvc.perform(post("/agents/register")
                     .header("Authorization", "Bearer " + rootUserToken) // don't forget your token!
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(agentRegistrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workEmail").value(testAgentEmail))
                .andExpect(jsonPath("$.fullName").value(testAgentNameFirst + " " + testAgentNameLast))
                .andExpect(jsonPath("$.uuid").exists());

        // --- Step 4: ensure the new user can log in
        AgentLoginRequest newAgentLoginRequest = new AgentLoginRequest(testAgentEmail, testAgentPass);

        MvcResult testUserLoginResult = mockMvc.perform(post("/agents/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newAgentLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workEmail").value(testAgentEmail))
                .andExpect(jsonPath("$.fullName").value(testAgentNameFirst + " " + testAgentNameLast))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String testUserLoginResponseBody = testUserLoginResult.getResponse().getContentAsString();
        String userLoginToken = objectMapper.readTree(testUserLoginResponseBody).get("token").asString();

        assertFalse(userLoginToken.isEmpty());
    }

    @Test
    void unauthorizedStrangerCantRegisterAsNewAgent() throws Exception {
        String testAgentNameFirst = "Bilbo";
        String testAgentNameLast = "Baggins";
        String testAgentNameEmail = "BigBaggins@gmail.com";
        String testAgentPassword = "pass123456789";

        AgentRegistrationRequest agentRegistrationRequest = new AgentRegistrationRequest(testAgentNameFirst, testAgentNameLast, null, testAgentNameEmail, null, testAgentPassword);

        mockMvc.perform(post("/agents/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(agentRegistrationRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rootUserCantLoginWithWrongPassword() throws Exception {

        String wrongPassword = rootPassword + "123";

        AgentLoginRequest agentLoginRequest = new AgentLoginRequest(rootEmail, wrongPassword);

        mockMvc.perform(post("/agents/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(agentLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    void rootUserCanDefineNewRole() throws Exception {
        String testRoleName = "SuperLoser";


        String rootUserToken = loginAsRoot();
    }

}
