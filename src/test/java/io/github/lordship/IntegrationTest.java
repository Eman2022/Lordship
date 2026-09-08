package io.github.lordship;


import io.github.lordship.identity.AgentAuthorizationCache;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSupport.class)
public abstract class IntegrationTest {

    @Autowired
    protected TestDataSupport testData;

    @Autowired
    protected AgentAuthorizationCache authorizationCache;

    @BeforeEach
    void clearAuthorizationCache() {
        authorizationCache.invalidateAll();
    }
}