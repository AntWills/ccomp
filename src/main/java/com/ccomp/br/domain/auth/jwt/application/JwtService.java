package com.ccomp.br.domain.auth.jwt.application;

import com.ccomp.br.domain.auth.core.dto.ClientMetadataDTO;
import com.ccomp.br.domain.auth.core.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.jwt.persistence.RefreshToken;
import com.ccomp.br.domain.auth.jwt.persistence.RefreshTokenRepository;
import com.ccomp.br.domain.auth.passwordreset.application.TokenHasher;
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
import java.util.Objects;
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
    public UUID createRefreshToken(UUID userId, ClientMetadataDTO clientMetadata){
        String safeUserAgent = truncate(clientMetadata.userAgent(), 500);
        String safeIpAddress = truncate(clientMetadata.ipAddress(), 45);

        List<RefreshToken> sameDeviceTokens = refreshTokenRepository.findAllByUserId(userId).stream()
                .filter(t -> Objects.equals(t.getUserAgent(), safeUserAgent))
                .toList();

        refreshTokenRepository.deleteAll(sameDeviceTokens);
        refreshTokenRepository.flush();

        UUID rawToken = UUID.randomUUID();

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(TokenHasher.hash(rawToken))
                .ipAddress(safeIpAddress)
                .userAgent(safeUserAgent)
                .expiryDate(Instant.now().plusSeconds(refreshExpirationInSeconds))
                .build());

        return rawToken;
    }

    @Transactional
    public Optional<String> validRefreshToken(RefreshTokenRequest request) {
        String hash = TokenHasher.hash(request.refreshToken());
        RefreshToken refresh = refreshTokenRepository.findByTokenHash(hash)
                        .orElseThrow(() -> new InvalidTokenException("Sessão inválida. Faça login novamente."));

        if (refresh.isTokenExpired()) {
            refreshTokenRepository.delete(refresh);
            return Optional.empty();
        }

        if(!userManagement.isAccountActive(refresh.getUserId())) {
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
