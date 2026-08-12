package com.ccomp.br.domain.security.jwt.application;

import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.security.jwt.persistence.RefreshToken;
import com.ccomp.br.domain.security.jwt.persistence.RefreshTokenRepository;
import com.ccomp.br.domain.users.external.RolesServices;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserManagement userManagement;

    public JwtService(RefreshTokenRepository refreshTokenRepository, JwtEncoder jwtEncoder, RolesServices rolesServices, UserManagement userManagement) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtEncoder = jwtEncoder;
        this.rolesServices = rolesServices;
        this.userManagement = userManagement;
    }

    public String generateAccessToken(UUID userId, List<String> roles) {
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

    @Transactional
    public RefreshToken getRefreshToken(UUID userId){
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(refreshExpirationInSeconds))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public Optional<String> validRefreshToken(RefreshTokenRequest request) {
        RefreshToken refresh =
                refreshTokenRepository.findByToken(request.refreshToken())
                        .orElseThrow(() -> new InvalidTokenException("Tempo de acesso expirado."));

        if(!userManagement.isAccountActive(refresh.getUserId())) {
            refreshTokenRepository.delete(refresh);
            return Optional.empty();
        }

        if (refresh.isTokenExpired()) {
            refreshTokenRepository.delete(refresh);
            return Optional.empty();
        }

        List<String> roles = rolesServices.loadRolesByUserID(refresh.getUserId()).stream()
                .map(role -> "ROLE_" + role.name())
                .toList();

        return Optional.of(generateAccessToken(refresh.getUserId(), roles));
    }

    @Transactional
    public void deleteRefreshToken(RefreshTokenRequest request){
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    @Transactional
    public void deleteRefreshTokenByUserId(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
