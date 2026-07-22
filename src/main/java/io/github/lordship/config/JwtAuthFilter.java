package io.github.lordship.config;


import io.github.lordship.access.*;
import io.github.lordship.identity.AgentPrincipal;
import io.github.lordship.identity.LordshipPrincipal;
import io.github.lordship.propertyassignments.PropertyAssignmentService;
import io.github.lordship.shared.PropertyScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;



public class JwtAuthFilter extends OncePerRequestFilter {

    //protected final Log logger = LogFactory.getLog(getClass());

    private final JwtService jwtService;
    private final PermissionResolverService permissionResolverService;
    private final AgentService agentService;
    private final PropertyAssignmentService propertyAssignmentService;

    public JwtAuthFilter(JwtService jwtService,
                         PermissionResolverService permissionResolverService,
                         AgentService agentService,
                         PropertyAssignmentService propertyAssignmentService) {
        this.jwtService = jwtService;
        this.permissionResolverService = permissionResolverService;
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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID agentId = jwtService.extractAgentId(token);
        String userType = jwtService.extractUserType(token);

        Set<SimpleGrantedAuthority> authorities;
        LordshipPrincipal principal;

        if ("AGENT".equals(userType)) {
            Agent agent = agentService.findById(agentId).orElseThrow();

            authorities = permissionResolverService.findPermissionsForAgent(agentId)
                    .stream()
                    .map(p -> new SimpleGrantedAuthority(p.permissionName()))
                    .collect(Collectors.toSet());

            boolean assignAll = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("assignments:assign-all"));

            PropertyScope scope = assignAll
                    ? new PropertyScope.All()
                    : new PropertyScope.Restricted(propertyAssignmentService.getAgentAssignedPropertyUUIDs(agentId));
            principal = new AgentPrincipal(agent.uuid(), agent.personId(), scope);
        } else {
            throw new IllegalArgumentException("Unsupported user type: " + userType);
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}