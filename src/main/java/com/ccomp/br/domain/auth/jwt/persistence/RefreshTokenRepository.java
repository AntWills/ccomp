package com.ccomp.br.domain.auth.jwt.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findAllByUserId(UUID userId);
    void deleteByTokenHash(String tokenHash);
    void deleteByUserId(UUID userId);
}