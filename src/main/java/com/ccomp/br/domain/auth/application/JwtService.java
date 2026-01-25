package com.ccomp.br.domain.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JwtService {
    @Value("${jwt.expiration}")
    private long expirationInSeconds;
    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String getAccessToken(UUID id) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationInSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("com.ccomp.br")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(id.toString())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long getExpirationInSeconds() {
        return expirationInSeconds;
    }
}
