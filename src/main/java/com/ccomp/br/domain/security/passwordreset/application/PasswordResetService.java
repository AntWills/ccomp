package com.ccomp.br.domain.security.passwordreset.application;

import com.ccomp.br.domain.security.passwordreset.persistence.PasswordResetToken;
import com.ccomp.br.domain.security.passwordreset.persistence.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public String issuePasswordResetToken(UUID userId) {
        String token = TokenGenerator.generateToken();
        log.info("Token: {}", token);
        String hash = TokenHasher.hash(token);

        passwordResetTokenRepository.save(
                PasswordResetToken.builder()
                        .hashId(hash)
                        .userId(userId)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .build()
        );
        return token;
    }

    @Transactional
    public Optional<UUID> validateAndConsumeToken(String token) {
        String hash = TokenHasher.hash(token);

        var passwordResetToken = passwordResetTokenRepository.findById(hash);

        Optional<UUID> userId = passwordResetToken
                .filter(PasswordResetToken::isValid)
                .map(PasswordResetToken::getUserId);

        passwordResetToken.ifPresent(passwordResetTokenRepository::delete);

        return userId;
    }
}
