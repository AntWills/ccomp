package com.ccomp.br.domain.auth.passwordreset.application;

import com.ccomp.br.domain.auth.passwordreset.persistence.PasswordResetToken;
import com.ccomp.br.domain.auth.passwordreset.persistence.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private UUID userId;
    private String rawToken;
    private PasswordResetToken mockedTokenEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rawToken = "raw-random-token-123";

        mockedTokenEntity = mock(PasswordResetToken.class);
    }

    @Nested
    @DisplayName("Issue Password Reset Token - Emissão de Token")
    class IssuePasswordResetToken {

        @Test
        @DisplayName("Remove tokens antigos e salva o novo token gerado com sucesso")
        void issuePasswordResetToken_deletesOldTokensAndSavesNewOne() {
            String generatedToken = passwordResetService.issuePasswordResetToken(userId);

            assertThat(generatedToken).isNotNull().isNotBlank();

            verify(passwordResetTokenRepository).deleteAllByUserId(userId);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("Configura corretamente os metadados do token (hash, usuário e expiração) antes de salvar")
        void issuePasswordResetToken_configuresTokenMetadataCorrectly() {
            passwordResetService.issuePasswordResetToken(userId);

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(captor.capture());

            PasswordResetToken savedToken = captor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(userId);
            assertThat(savedToken.getHashId()).isNotNull().isNotBlank();
            assertThat(savedToken.getCreatedAt()).isNotNull();
            assertThat(savedToken.getExpiresAt()).isNotNull()
                    .isAfter(savedToken.getCreatedAt()); // Verifica se a expiração está no futuro
        }
    }

    @Nested
    @DisplayName("Validate and Consume Token - Validação e Consumo")
    class ValidateAndConsumeToken {

        @Test
        @DisplayName("Retorna o UUID do usuário e deleta o token quando ele for válido")
        void validateAndConsumeToken_returnsUserIdAndDeletesToken_whenTokenIsValid() {
            // Simula a busca no banco por qualquer Hash devolvendo nossa entidade mockada
            when(passwordResetTokenRepository.findById(anyString())).thenReturn(Optional.of(mockedTokenEntity));
            when(mockedTokenEntity.isValid()).thenReturn(true);
            when(mockedTokenEntity.getUserId()).thenReturn(userId);

            Optional<UUID> result = passwordResetService.validateAndConsumeToken(rawToken);

            assertThat(result).isPresent().contains(userId);

            // Garante que o token foi deletado após o consumo (uso único)
            verify(passwordResetTokenRepository).delete(mockedTokenEntity);
        }

        @Test
        @DisplayName("Retorna Optional.empty() e ignora a validação quando o token for nulo ou em branco")
        void validateAndConsumeToken_returnsEmpty_whenTokenIsNullOrBlank() {
            Optional<UUID> resultNull = passwordResetService.validateAndConsumeToken(null);
            Optional<UUID> resultBlank = passwordResetService.validateAndConsumeToken("   ");

            assertThat(resultNull).isEmpty();
            assertThat(resultBlank).isEmpty();

            // Garante que o repositório sequer foi chamado
            verify(passwordResetTokenRepository, never()).findById(anyString());
            verify(passwordResetTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Retorna Optional.empty() mas deleta a entidade do banco quando o token estiver expirado/inválido")
        void validateAndConsumeToken_returnsEmptyAndDeletesToken_whenTokenIsExpired() {
            // Simula a entidade sendo encontrada no banco, porém expirada
            when(passwordResetTokenRepository.findById(anyString())).thenReturn(Optional.of(mockedTokenEntity));
            when(mockedTokenEntity.isValid()).thenReturn(false);

            Optional<UUID> result = passwordResetService.validateAndConsumeToken(rawToken);

            assertThat(result).isEmpty();

            // Garante que o token expirado/inválido seja limpo do banco
            verify(passwordResetTokenRepository).delete(mockedTokenEntity);
            verify(mockedTokenEntity, never()).getUserId(); // Não deve tentar extrair o ID
        }
    }
}