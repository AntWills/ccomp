package com.ccomp.br.domain.security.passwordreset.application;

import com.ccomp.br.domain.security.passwordreset.persistence.PasswordResetToken;
import com.ccomp.br.domain.security.passwordreset.persistence.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public String issuePasswordResetToken(UUID userId) {
        String token = TokenGenerator.generateToken();
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
    public boolean validateAndConsumeToken(String token, UUID userId) {
        String hash = TokenHasher.hash(token);

        var passwordResetToken = passwordResetTokenRepository.findById(hash);

        boolean result = passwordResetToken
                .filter(t -> t.getUserId().equals(userId))
                .filter(PasswordResetToken::isValid)
                .isPresent();

        passwordResetToken.ifPresent(passwordResetTokenRepository::delete);

        return result;
    }
}
