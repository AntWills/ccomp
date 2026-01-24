package com.ccomp.br.domain.auth.application;

import com.ccomp.br.config.JwtConfig;
import com.ccomp.br.domain.auth.dto.AuthResponse;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.domain.users.management.UserManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthApplication {
    private final UserManagement userManagement;
    private final JwtConfig jwtConfig;
    private final JwtEncoder jwtEncoder;

    @Autowired
    public AuthApplication(UserManagement userManagement, JwtConfig jwtConfig, JwtEncoder jwtEncoder){
        this.userManagement = userManagement;
        this.jwtConfig = jwtConfig;
        this.jwtEncoder = jwtEncoder;
    }

    public AuthResponse register(RegisterUserDTO dto){
        var userDto = userManagement.register(dto);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtConfig.getExpirationInSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("com.comp.br")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(userDto.id().toString())
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new AuthResponse(accessToken, jwtConfig.getExpirationInSeconds()); // Terminar depois
    }
}
