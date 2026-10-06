package com.ccomp.br.domain.auth.security;

import com.ccomp.br.domain.auth.core.application.AuthApplication;
import com.ccomp.br.domain.auth.core.dto.ClientMetadataDTO;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthApplication authApplication;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String email = googleUser.getAttribute("email");
        Boolean emailVerified = googleUser.getAttribute("email_verified");
        String subject = googleUser.getAttribute("sub");
        String name = googleUser.getAttribute("name");

        if (email == null || !Boolean.TRUE.equals(emailVerified) || subject == null || subject.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google não retornou uma identidade verificada.");
            return;
        }
        if (name == null || name.isBlank()) name = email;

        var tokens = authApplication.signInWithGoogle(email, name, subject,
                ClientMetadataDTO.builder()
                        .ipAddress(request.getRemoteAddr())
                        .userAgent(request.getHeader("User-Agent"))
                        .build());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), tokens);
    }
}
