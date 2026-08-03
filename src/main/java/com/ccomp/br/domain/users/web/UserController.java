package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.security.roles.application.RolesServices;
import com.ccomp.br.domain.security.roles.enums.EnumRoles;
import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.shared.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de usuários e controle de permissões (roles)")
public class UserController {

    private final UserApplication userApplication;
    private final RolesServices rolesServices;

    @Autowired
    public UserController(UserApplication userApplication, RolesServices rolesServices) {
        this.userApplication = userApplication;
        this.rolesServices = rolesServices;
    }

    @Operation(
            summary = "Listar todos os usuários",
            description = "Retorna uma lista com todos os usuários cadastrados na aplicação. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN")
    })
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userApplication.getAll());
    }

    @Operation(
            summary = "Atribuir role a um usuário",
            description = "Adiciona um perfil de acesso/role específico a um usuário. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role atribuída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário ou Role não encontrada")
    })
    @PostMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addRole(
            @Parameter(description = "ID do usuário (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,

            @Parameter(description = "Perfil/Role a ser concedido", example = "ADMIN")
            @PathVariable EnumRoles role
    ) {
        rolesServices.addRole(userId, role);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Remover role de um usuário",
            description = "Revoga um perfil de acesso/role de um usuário. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role removida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário ou Role não encontrada")
    })
    @DeleteMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "ID do usuário (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,

            @Parameter(description = "Perfil/Role a ser removido", example = "ADMIN")
            @PathVariable EnumRoles role
    ) {
        rolesServices.removeRole(userId, role);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Obter dados do usuário logado",
            description = "Retorna os detalhes do perfil do usuário autenticado no momento através do Token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return userApplication.getById(UUID.fromString(jwt.getSubject()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}