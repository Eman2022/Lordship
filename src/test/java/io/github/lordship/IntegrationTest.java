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

    // Removing this @beforeEach annotation should leave the suite green — afterCompletion cleans up after rolled-back tests. If it doesn't, an eviction path has gone missing.
    @BeforeEach
    void clearAuthorizationCache() {
        authorizationCache.invalidateAll();
    }
}