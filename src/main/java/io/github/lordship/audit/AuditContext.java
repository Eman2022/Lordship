package io.github.lordship.audit;


import io.github.lordship.shared.UserType;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuditContext {

    private final UUID correlationId = UUID.randomUUID();
    private UUID actingUserId; // could be from a user OR an agent
    private String ipAddress;
    private UserType userType;

    public UUID getCorrelationId() { return correlationId; }

    public UUID getActingUserId() { return actingUserId; }

    public void setActingUserId(UUID actingUserId) { this.actingUserId = actingUserId; }

    public String getIpAddress() { return ipAddress; }

    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public void setUserType(UserType userType) { this.userType = userType; }

    public UserType getUserType() { return userType; }

}