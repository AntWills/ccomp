package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.domain.users.dto.UpdateUserDTO;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/users")
@Tag(name = "Perfil do Usuários", description = "Endpoints para gerenciamento de usuários e controle de permissões (roles)")
public class UserProfileController {

    private final UserApplication userApplication;

    @Autowired
    public UserProfileController(UserApplication userApplication) {
        this.userApplication = userApplication;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Obter dados do usuário logado",
            description = "Retorna os detalhes do perfil do usuário autenticado no momento através do Token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<UserDTO> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return userApplication.getById(UUID.fromString(jwt.getSubject()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping
    @Operation(
            summary = "Atualizar os dados do usuário logado",
            description = "Recebe os dados e retorna os detalhes do perfil do usuário autenticado no momento através do Token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso após atualização"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<UserDTO> update(
            @Valid @RequestBody UpdateUserDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userApplication.update(extractUserId(jwt), dto));
    }

    @DeleteMapping
    @Operation(
            summary = "Desativa a conta do usuário atual",
            description = "Está ação vai desativar a conta do usuário, mas não irá apagar seus dados. Basta realizar o login novamente para a conta ser reativada. Até lá a conta não vai estar mais recebendo email ou nóticias da plataforma."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conta desativada"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<MessageResponse> deactivateAccount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        userApplication.deactivateOwnAccount(UUID.fromString(jwt.getSubject()));

        return ResponseEntity.ok(
                new MessageResponse("Conta desativada com sucesso!"));
    }

    private UUID extractUserId(Jwt jwt) {
        return jwt != null ? UUID.fromString(jwt.getSubject()) : null;
    }
}