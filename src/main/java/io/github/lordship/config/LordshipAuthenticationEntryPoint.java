package io.github.lordship.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * 401 for a request that arrived without a usable identity.
 *
 * <p>Spring's default entry point when no login mechanism is configured is
 * {@code Http403ForbiddenEntryPoint}, which is why an expired token used to come
 * back looking exactly like a missing permission. With this in place the two are
 * distinguishable from the status line alone: 401 means the token is absent,
 * malformed or expired, 403 means an authenticated agent lacks the authority.
 */
public class LordshipAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        send(response, null, "Authentication required");
    }

    /**
     * Writes the 401. {@code error} is the RFC 6750 code for the
     * WWW-Authenticate challenge, or null when no credentials were presented at
     * all. Descriptions must not contain double quotes -- they go into the
     * header verbatim.
     */
    public static void send(HttpServletResponse response, String error, String description) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, error == null
                ? "Bearer"
                : "Bearer error=\"" + error + "\", error_description=\"" + description + "\"");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + description + "\"}");
    }
}