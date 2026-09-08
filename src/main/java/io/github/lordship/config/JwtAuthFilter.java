package io.github.lordship.config;

import io.github.lordship.access.*;
import io.github.lordship.identity.AgentPrincipal;
import io.github.lordship.identity.LordshipPrincipal;
import io.github.lordship.propertyassignments.PropertyAssignmentService;
import io.github.lordship.shared.PropertyScope;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A token that is present but unusable is answered here with a 401 and a reason,
 * rather than falling through to anonymous and being denied later as though a
 * permission were missing. No header at all still falls through: the request may
 * be for a permitAll path, and if it is not, the entry point turns the denial
 * into a 401 of its own.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PermissionService permissionService;
    private final AgentService agentService;
    private final PropertyAssignmentService propertyAssignmentService;

    public JwtAuthFilter(JwtService jwtService,
                         PermissionService permissionService,
                         AgentService agentService,
                         PropertyAssignmentService propertyAssignmentService) {
        this.jwtService = jwtService;
        this.permissionService = permissionService;
        this.agentService = agentService;
        this.propertyAssignmentService = propertyAssignmentService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authHeader.startsWith("Bearer ")) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_request",
                    "Authorization header must use the Bearer scheme");
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_request",
                    "Bearer token is empty");
            return;
        }

        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (ExpiredJwtException e) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_token",
                    "Access token expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_token",
                    "Access token is malformed or its signature does not verify");
            return;
        }

        String userType = jwtService.extractUserType(claims);

        if (!"AGENT".equals(userType)) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_token",
                    "Unsupported user type in token");
            return;
        }

        UUID agentId;
        try {
            agentId = jwtService.extractAgentId(claims);
        } catch (IllegalArgumentException | NullPointerException e) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_token",
                    "Token subject is not an agent id");
            return;
        }

        // A token outliving the agent it names is a stale credential, not a
        // permission problem, so it gets the same 401 as an expired one.
        Optional<Agent> agentOpt = agentService.findById(agentId);
        if (agentOpt.isEmpty()) {
            LordshipAuthenticationEntryPoint.send(response, "invalid_token",
                    "The agent this token identifies no longer exists");
            return;
        }
        Agent agent = agentOpt.get();

        // Sessions ended by a password change or an explicit revoke. The token is
        // still signed and still unexpired, so nothing above catches it.
        if (agent.tokensValidFrom() != null) {
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt == null || issuedAt.toInstant().isBefore(agent.tokensValidFrom().toInstant())) {
                LordshipAuthenticationEntryPoint.send(response, "invalid_token",
                        "Session ended, sign in again");
                return;
            }
        }

        Set<SimpleGrantedAuthority> authorities = permissionService.findPermissionsForAgent(agentId)
                .stream()
                .map(p -> new SimpleGrantedAuthority(p.permissionName()))
                .collect(Collectors.toSet());

        boolean assignAll = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("assignments:assign-all"));

        PropertyScope scope = assignAll
                ? new PropertyScope.All()
                : new PropertyScope.Restricted(propertyAssignmentService.getAgentAssignedPropertyUUIDs(agentId));

        LordshipPrincipal principal = new AgentPrincipal(agent.uuid(), agent.personId(), scope);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}