package com.ccomp.br.domain.auth.web;

import com.ccomp.br.domain.auth.application.AuthApplication;
import com.ccomp.br.domain.auth.dto.RefreshTokenRequest;
import com.ccomp.br.domain.auth.dto.RefreshTokenResponse;
import com.ccomp.br.domain.auth.dto.SignInResponse;
import com.ccomp.br.domain.auth.dto.LoginRequestDTO;
import com.ccomp.br.shared.dto.RegisterUserDTO;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Operações relacionadas ao registro da conta, login e token de acesso.")
@RestController
@Slf4j
@RequestMapping("/auth")
public class AuthController {
    private final AuthApplication authApplication;

    @Autowired
    public AuthController(AuthApplication authApplication) {
        this.authApplication = authApplication;
    }

    @Transactional
    @PostMapping("/sign-up")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserDTO dto) {
        authApplication.signUp(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body("Conta criada com sucesso.");
    }

    @Operation(
            summary = "Retorna o access token junto com o tempo de expiração do mesmo.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login realizado com sucesso.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SignInResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Credenciais inválidas.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @PostMapping("/sign-in")
    public SignInResponse login(@Valid @RequestBody LoginRequestDTO dto) {
        return authApplication.signIn(dto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){
        return authApplication.refresh(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request){

        return ResponseEntity.notFound().build();
    }
}
