package io.github.lordship.config;


import io.github.lordship.access.JwtService;
import io.github.lordship.access.PermissionResolverService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

//NOTE: this class is NOT put in the internal folder because the SecurityConfig file needs it

public class JwtAuthFilter extends OncePerRequestFilter {

    protected final Log logger = LogFactory.getLog(getClass());

    private final JwtService jwtService;
    private final PermissionResolverService permissionResolverService;

    public JwtAuthFilter(JwtService jwtService, PermissionResolverService permissionResolverService) {
        this.jwtService = jwtService;
        this.permissionResolverService = permissionResolverService;
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

        Set<SimpleGrantedAuthority> authorities = permissionResolverService.findPermissionsForAgent(agentId)
                .stream()
                .map(p -> new SimpleGrantedAuthority(p.permissionName()))
                .collect(Collectors.toSet());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        jwtService.extractAgentId(token),
                        null,
                        authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}