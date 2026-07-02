package io.github.lordship.config;

import io.github.lordship.identity.AgentPrincipal;
import io.github.lordship.identity.LordshipPrincipal;
import io.github.lordship.audit.AuditContext;
import io.github.lordship.shared.UserType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


public class AuditContextFilter extends OncePerRequestFilter {

    private final AuditContext auditContext;

    public AuditContextFilter(AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof LordshipPrincipal principal) {
            switch (principal){
                case AgentPrincipal agentPrincipal -> {
                    auditContext.setActingUserId(agentPrincipal.agentUuid());
                    auditContext.setUserType(UserType.AGENT);
                }
                default -> throw new IllegalStateException("Unexpected value: " + principal);
            }
        }

        auditContext.setIpAddress(request.getRemoteAddr());
        filterChain.doFilter(request, response);
    }
}
