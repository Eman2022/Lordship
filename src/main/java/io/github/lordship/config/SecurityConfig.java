package io.github.lordship.config;

import io.github.lordship.access.AgentService;
import io.github.lordship.access.JwtService;
import io.github.lordship.access.PermissionResolverService;
import io.github.lordship.audit.AuditContext;
import io.github.lordship.propertyassignments.PropertyAssignmentService;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuditContextFilter auditContextFilter(AuditContext auditContext) {
        return new AuditContextFilter(auditContext);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService,
                                       PermissionResolverService permissionResolverService,
                                       AgentService agentService,
                                       PropertyAssignmentService propertyAssignmentService) {
        return new JwtAuthFilter(jwtService, permissionResolverService, agentService, propertyAssignmentService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Qualifier("jwtAuthFilter") Filter jwtAuthFilter,
                                           @Qualifier("auditContextFilter") Filter auditContextFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/agents/auth").permitAll()
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditContextFilter, jwtAuthFilter.getClass());
        return http.build();
    }
}