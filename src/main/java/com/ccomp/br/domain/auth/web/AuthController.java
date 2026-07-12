package com.ccomp.br.domain.auth.web;

import com.ccomp.br.domain.auth.application.AuthApplication;
import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.dto.RefreshTokenResponse;
import com.ccomp.br.domain.auth.dto.AccessTokenResponse;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Autenticação", description = "Operações relacionadas ao registro da conta, login e token de acesso.")
@RestController
@Slf4j
@RequestMapping("api/auth")
public class AuthController {
    private final AuthApplication authApplication;

    @Autowired
    public AuthController(AuthApplication authApplication) {
        this.authApplication = authApplication;
    }

    @Operation(
            summary = "Cria uma nova conta de usuário",
            description = "Registra um novo usuário no sistema. " +
                    "O e-mail deve ser único e a senha deve atender aos critérios mínimos de segurança.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Conta criada com sucesso",
                            content = @Content(
                                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                                    schema = @Schema(type = "string", example = "Conta criada com sucesso.")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados inválidos, campos obrigatórios ausentes.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Conflito - e-mail já está em uso",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody RegisterUserDTO dto) {
        authApplication.signUp(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Conta criada com sucesso.")
        );
    }

    @Operation(
            summary = "Retorna o access token junto com o tempo de expiração do mesmo.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login realizado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AccessTokenResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Credenciais inválidas.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @PostMapping("/sign-in")
    public AccessTokenResponse login(@Valid @RequestBody LoginRequestDTO dto) {
        return authApplication.signIn(dto);
    }

    @Operation(
            summary = "Renova o access token utilizando um refresh token válido",
            description = "Recebe um refresh token e retorna um novo access token (e opcionalmente novo refresh token).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tokens renovados com sucesso",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RefreshTokenResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou refresh token mal formatado",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Refresh token inválido, expirado ou revogado",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authApplication.refresh(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Operation(
            summary = "Realiza logout / invalidação do refresh token",
            description = "Marca o refresh token informado como inválido/revogado, " +
                    "impedindo sua utilização futura. " +
                    "(O access token continua válido até expirar)",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Logout realizado com sucesso (token invalidado)"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou refresh token ausente/mal formatado",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authApplication.logout(request);
        return ResponseEntity.noContent().build();
    }
}
