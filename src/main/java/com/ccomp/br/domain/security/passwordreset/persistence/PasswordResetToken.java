package com.ccomp.br.domain.security.passwordreset.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_password_reset_token")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    @Column(name = "hash_id", nullable = false, updatable = false)
    private String hashId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isValid() {
        return expiresAt.isAfter(LocalDateTime.now());
    }
}
