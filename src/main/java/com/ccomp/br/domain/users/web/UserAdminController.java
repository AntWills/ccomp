package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.AdminServices;
import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.domain.users.dto.BlockAccountReq;
import com.ccomp.br.domain.users.dto.UserItemView;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.utils.CursorPage;
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

import java.util.UUID;

@Tag(
        name = "Administração de Usuários",
        description = "Endpoints de gestão de contas para ADMIN e STAFF")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final AdminServices adminServices;
    private final UserApplication userApplication;

    @Autowired
    public UserAdminController(AdminServices adminServices, UserApplication userApplication) {
        this.adminServices = adminServices;
        this.userApplication = userApplication;
    }

    @PostMapping("/search")
    @Operation(
            summary = "Listar todos os usuários",
            description = "Retorna uma lista, junto com o cursor, de todos os usuários cadastrados na aplicação. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN")
    })
    public ResponseEntity<CursorPage<UserItemView>> searchUsers(
            @Valid @RequestBody UserSearchFilter filter,
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(adminServices.searchUsers(filter, nextCursor, pageSize));
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Obter dados do usuário via ID",
            description = "Retorna os detalhes do perfil do usuário usando o UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<UserDTO> getById(@PathVariable UUID userId) {
        return userApplication.getById(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("email/{email}")
    @Operation(
            summary = "Obter dados do usuário via email",
            description = "Retorna os detalhes do perfil do usuário usando o email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<UserDTO> getByEmail(@PathVariable EmailAddress email) {
        return adminServices.getByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("{userId}/block")
    @Operation(
            summary = "Bloqueia o usuário",
            description = "O perfil do usuário será bloqueado, com todas as suas credenciais suspensas e impedido de fazer login."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário bloqueado"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<MessageResponse> blockByEmail(
            @PathVariable UUID userId,
            @Valid @RequestBody BlockAccountReq req,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        adminServices.blockUser(userId, req.reason(), UUID.fromString(jwt.getSubject()));

        return ResponseEntity.ok(
                new MessageResponse("Usuário bloqueado com sucesso.")
        );
    }

    @PatchMapping("{userId}/unlock")
    @Operation(
            summary = "Desbloqueia o usuário",
            description = "O perfil do usuário que está bloqueado, será desbloqueado e retornado como ativo (ACTIVE) para a plataforma."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário bloqueado"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    public ResponseEntity<MessageResponse> unlockByEmail(
            @PathVariable UUID userId,
            @Valid @RequestBody BlockAccountReq req,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        adminServices.unlockUser(userId, req.reason(), UUID.fromString(jwt.getSubject()));

        return ResponseEntity.ok(
                new MessageResponse("Usuário desbloqueado com sucesso.")
        );
    }

    @PutMapping("/{userId}/roles/{role}")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Mapipular cargos (Roles) de um usuário",
            description = "Alterar um perfil de acesso/role específico a um usuário. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário ou Role não encontrada")
    })
    public ResponseEntity<MessageResponse> changeRole(
            @Parameter(description = "ID do usuário (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,

            @Parameter(description = "Perfil/Role a ser concedido", example = "ADMIN")
            @PathVariable EnumRoles role,

            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        adminServices.changeRole(userId, role, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok(
                new MessageResponse("Cargo alterado com sucesso.")
        );
    }
}
