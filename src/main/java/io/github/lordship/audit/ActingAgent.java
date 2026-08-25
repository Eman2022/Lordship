package io.github.lordship.audit;

import io.github.lordship.shared.SystemPrincipal;
import io.github.lordship.shared.UserType;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;


 //Answers "who is doing this" for a NOT NULL created_by column.

public final class ActingAgent {

    private ActingAgent() { }

    /** The authenticated agent, or the SYSTEM principal when there is nobody to attribute to. */
    public static UUID resolve(AuditContext auditContext) {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return SystemPrincipal.AGENT_UUID;
        }
        UUID acting = auditContext.getActingUserId();
        return acting != null ? acting : SystemPrincipal.AGENT_UUID;
    }

}
