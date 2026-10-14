package com.ccomp.br.domain.auth.core.application;

import com.ccomp.br.shared.exceptions.InvalidTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
public class GoogleIdentityVerifier {
    private final JwtDecoder googleIdTokenDecoder;

    public GoogleIdentityVerifier(@Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder) {
        this.googleIdTokenDecoder = googleIdTokenDecoder;
    }

    public GoogleIdentity verify(String credential) {
        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(credential);
        } catch (JwtException ex) {
            throw new InvalidTokenException("A credencial Google é inválida ou expirou.");
        }

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        Boolean emailVerified = jwt.getClaim("email_verified");

        if (email == null || email.isBlank() || jwt.getSubject() == null || jwt.getSubject().isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {
            throw new InvalidTokenException("Google não retornou uma identidade com e-mail verificado.");
        }
        if (name == null || name.isBlank()) {
            name = email;
        }

        return new GoogleIdentity(email, name);
    }

    public record GoogleIdentity(String email, String name) {
    }
}
