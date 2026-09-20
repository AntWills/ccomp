package com.ccomp.br.domain.auth.jwt.application;

import com.ccomp.br.domain.auth.core.dto.ClientMetadataDTO;
import com.ccomp.br.domain.auth.core.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.jwt.persistence.RefreshToken;
import com.ccomp.br.domain.auth.jwt.persistence.RefreshTokenRepository;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.external.RolesServices;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private RolesServices rolesServices;

    @Mock
    private UserManagement userManagement;

    @InjectMocks
    private JwtService jwtService;

    private UUID userId;
    private UUID rawToken;
    private RefreshToken existingRefreshToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rawToken = UUID.randomUUID();

        // Injeta os valores das propriedades @Value simulando o application.properties
        ReflectionTestUtils.setField(jwtService, "accessExpirationInSeconds", 3600L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationInSeconds", 86400L);

        existingRefreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash("hashed-token-string")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .expiryDate(Instant.now().plusSeconds(86400L))
                .build();
    }

    @Nested
    @DisplayName("Generate Access Token - Geração do JWT")
    class GenerateAccessToken {

        @Test
        @DisplayName("Gera o token de acesso com sucesso para o usuário com suas roles")
        void generateAccessToken_returnsTokenString_whenValidDataProvided() {
            List<String> roles = List.of("ROLE_ADMIN", "ROLE_USER");
            Jwt mockJwt = mock(Jwt.class);

            when(mockJwt.getTokenValue()).thenReturn("mocked-jwt-token");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            String token = jwtService.generateAccessToken(userId, roles);

            assertThat(token).isEqualTo("mocked-jwt-token");
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        }

        @Test
        @DisplayName("Gera o token de acesso com sucesso mesmo se a lista de roles estiver vazia")
        void generateAccessToken_returnsTokenString_whenRolesListIsEmpty() {
            List<String> emptyRoles = List.of();
            Jwt mockJwt = mock(Jwt.class);

            when(mockJwt.getTokenValue()).thenReturn("mocked-jwt-token-empty-roles");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            String token = jwtService.generateAccessToken(userId, emptyRoles);

            assertThat(token).isEqualTo("mocked-jwt-token-empty-roles");
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        }
    }

    @Nested
    @DisplayName("Create Refresh Token - Criação e Salva do Refresh Token")
    class CreateRefreshToken {

        @Test
        @DisplayName("Cria um novo refresh token, limpa sessões do mesmo dispositivo e retorna o UUID bruto")
        void createRefreshToken_createsTokenAndClearsOldDeviceSessions() {
            ClientMetadataDTO metadata = new ClientMetadataDTO("192.168.0.1", "Google Chrome");

            // Simula a busca de tokens antigos (1 token existente do mesmo dispositivo)
            RefreshToken oldToken = RefreshToken.builder().userAgent("Google Chrome").build();
            when(refreshTokenRepository.findAllByUserId(userId)).thenReturn(List.of(oldToken));

            UUID resultRawToken = jwtService.createRefreshToken(userId, metadata);

            assertThat(resultRawToken).isNotNull();

            // Verifica se o token antigo do mesmo dispositivo foi deletado
            verify(refreshTokenRepository).deleteAll(List.of(oldToken));
            verify(refreshTokenRepository).flush();

            // Verifica se o novo token foi salvo
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Trunca o User-Agent e IP caso excedam o tamanho máximo permitido pelo banco")
        void createRefreshToken_truncatesLargeMetadataFields() {
            String largeIp = "1".repeat(100); // 100 chars
            String largeUserAgent = "A".repeat(600); // 600 chars
            ClientMetadataDTO metadata = new ClientMetadataDTO(largeIp, largeUserAgent);

            jwtService.createRefreshToken(userId, metadata);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken savedToken = captor.getValue();
            assertThat(savedToken.getIpAddress()).hasSize(45);
            assertThat(savedToken.getUserAgent()).hasSize(500);
        }
    }

    @Nested
    @DisplayName("Valid Refresh Token - Validação e Renovação de Sessão")
    class ValidRefreshToken {

        @Test
        @DisplayName("Retorna um novo Access Token quando o refresh token é válido e a conta está ativa")
        void validRefreshToken_returnsNewAccessToken_whenTokenIsValid() {
            RefreshTokenRequest request = mock(RefreshTokenRequest.class);
            when(request.refreshToken()).thenReturn(rawToken);

            // Simula a busca do hash
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existingRefreshToken));
            when(userManagement.isAccountActive(userId)).thenReturn(true);

            // Mock das roles
            // Assumimos que rolesServices retorna uma coleção de objetos com o método name() (como uma Enum)
            when(rolesServices.loadRolesByUserID(userId)).thenReturn(List.of(EnumRoles.ADMIN));

            // Mock do encoder
            Jwt mockJwt = mock(Jwt.class);
            when(mockJwt.getTokenValue()).thenReturn("new-access-token");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            Optional<String> result = jwtService.validRefreshToken(request);

            assertThat(result).isPresent().contains("new-access-token");
            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Lança InvalidTokenException quando o token não é encontrado (hash inexistente)")
        void validRefreshToken_throwsException_whenTokenIsNotFound() {
            RefreshTokenRequest request = mock(RefreshTokenRequest.class);
            when(request.refreshToken()).thenReturn(rawToken);

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jwtService.validRefreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Sessão inválida. Faça login novamente.");
        }

        @Test
        @DisplayName("Deleta o token e retorna Optional.empty() quando o token está expirado")
        void validRefreshToken_returnsEmptyAndDeletesToken_whenTokenIsExpired() {
            RefreshTokenRequest request = mock(RefreshTokenRequest.class);
            when(request.refreshToken()).thenReturn(rawToken);

            existingRefreshToken.setExpiryDate(Instant.now().minusSeconds(100)); // Token expirado no passado
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existingRefreshToken));

            Optional<String> result = jwtService.validRefreshToken(request);

            assertThat(result).isEmpty();
            verify(refreshTokenRepository).delete(existingRefreshToken);
            verify(userManagement, never()).isAccountActive(any()); // Não deve chegar a validar a conta
        }
    }

    @Nested
    @DisplayName("Delete Refresh Token - Revogação de Sessões")
    class DeleteRefreshToken {

        @Test
        @DisplayName("Deleta o refresh token do banco realizando o hash do request")
        void deleteRefreshToken_deletesTokenByHash() {
            RefreshTokenRequest request = mock(RefreshTokenRequest.class);
            when(request.refreshToken()).thenReturn(rawToken);

            jwtService.deleteRefreshToken(request);

            verify(refreshTokenRepository).deleteByTokenHash(anyString());
        }
    }

    @Nested
    @DisplayName("Delete Refresh Token By UserId - Revogação Global")
    class DeleteRefreshTokenByUserId {

        @Test
        @DisplayName("Deleta todos os refresh tokens associados a um usuário")
        void deleteRefreshTokenByUserId_deletesAllTokensSuccessfully() {
            jwtService.deleteRefreshTokenByUserId(userId);

            verify(refreshTokenRepository).deleteByUserId(userId);
        }
    }
}