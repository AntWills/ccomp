package com.ccomp.br.domain.security.jwt.application;

import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.security.jwt.persistence.RefreshToken;
import com.ccomp.br.domain.security.jwt.persistence.RefreshTokenRepository;
import com.ccomp.br.domain.security.roles.application.RolesServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${jwt.access.expiration}")
    private long accessExpirationInSeconds;
    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationInSeconds;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtEncoder jwtEncoder;
    private final RolesServices rolesServices;

    public JwtService(RefreshTokenRepository refreshTokenRepository, JwtEncoder jwtEncoder, RolesServices rolesServices) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtEncoder = jwtEncoder;
        this.rolesServices = rolesServices;
    }

    public String getAccessToken(UUID userId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessExpirationInSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("com.ccomp.br")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(userId.toString())
                .claim("roles", roles)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public RefreshToken getRefreshToken(UUID userId){
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(refreshExpirationInSeconds))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<String> validRefreshToken(RefreshTokenRequest request) {
        Optional<RefreshToken> refreshOpt =
                refreshTokenRepository.findByToken(request.refreshToken());

        if (refreshOpt.isEmpty())
            return Optional.empty();

        RefreshToken refresh = refreshOpt.get();

        if (refresh.isTokenExpired()) {
            refreshTokenRepository.delete(refresh);
            return Optional.empty();
        }

        List<String> roles = rolesServices.loadRolesByUserID(refresh.getUserId()).stream()
                .map(role -> "ROLE_" + role.name())
                .toList();

        return Optional.of(getAccessToken(refresh.getUserId(), roles));
    }

    public void deleteRefreshToken(RefreshTokenRequest request){
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }
}
