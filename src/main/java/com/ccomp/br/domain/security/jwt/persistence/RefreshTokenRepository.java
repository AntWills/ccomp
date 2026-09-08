package com.ccomp.br.domain.security.jwt.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(UUID token);
    List<RefreshToken> findAllByUserId(UUID userId);
    void deleteByToken(UUID token);
    void deleteByUserId(UUID userId);
}