package com.ccomp.br.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public CustomAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.error("❌ Auth error: {} | Causa: {}",
                authException.getClass().getSimpleName(),
                authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = switch (authException) {
            case BadCredentialsException e -> "Email ou senha incorretos.";
            case UsernameNotFoundException e -> "Email ou senha incorretos.";
            case LockedException e -> "Email ou senha incorretos.";
            case DisabledException e -> "Email ou senha incorretos.";
            case InvalidBearerTokenException e -> "Token inválido ou expirado. Faça login novamente.";
            default -> "Não autorizado.";
        };

        var body = new ErrorResponse(LocalDateTime.now(), HttpServletResponse.SC_UNAUTHORIZED,
                "Não autorizado", request.getRequestURI(), List.of(message), null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
