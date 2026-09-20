package com.ccomp.br.domain.auth.passwordreset.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    void deleteAllByUserId(UUID userId);
}