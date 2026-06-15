package io.github.lordship.config;

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
import java.util.UUID;


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

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID agentId) {
            auditContext.setActingUserId(agentId);
            auditContext.setUserType(UserType.AGENT); //TODO: NOTE: assume they are all agents for now (will have tenants as users later)
        }

        auditContext.setIpAddress(request.getRemoteAddr());

        filterChain.doFilter(request, response);
    }
}
