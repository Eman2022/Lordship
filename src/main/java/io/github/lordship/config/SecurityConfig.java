package io.github.lordship.config;

import io.github.lordship.access.JwtService;
import io.github.lordship.audit.AuditContext;
import io.github.lordship.identity.AgentAuthorizationCache;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
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
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new LordshipAuthenticationEntryPoint();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService,
                                       AgentAuthorizationCache authorizationCache,
                                       AgentAuthorizationLoader authorizationLoader) {
        return new JwtAuthFilter(jwtService, authorizationCache, authorizationLoader);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Qualifier("jwtAuthFilter") Filter jwtAuthFilter,
                                           @Qualifier("auditContextFilter") Filter auditContextFilter,
                                           AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
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
                // Without this, the default is Http403ForbiddenEntryPoint and an
                // anonymous request is refused with the same 403 as an agent who
                // is logged in and simply lacks the authority. The entry point is
                // reached only when the denied request is anonymous; a real agent
                // still falls to the AccessDeniedHandler and its 403.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditContextFilter, jwtAuthFilter.getClass());
        return http.build();
    }
}