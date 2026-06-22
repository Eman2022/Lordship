package io.github.lordship.config;

import io.github.lordship.access.AgentService;
import io.github.lordship.access.JwtService;
import io.github.lordship.access.PermissionResolverService;
import io.github.lordship.audit.AuditContext;
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
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService, PermissionResolverService permissionResolverService, AgentService agentService) {
        return new JwtAuthFilter(jwtService, permissionResolverService, agentService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Qualifier("jwtAuthFilter") Filter jwtAuthFilter,
                                           @Qualifier("auditContextFilter") Filter auditContextFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/agents/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditContextFilter, jwtAuthFilter.getClass());
        return http.build();
    }
}