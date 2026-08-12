package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.AdminServices;
import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.domain.users.dto.BlockAccountReq;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.module.email.EmailAddress;
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
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class UserAdminController {
    private final AdminServices adminServices;
    private final UserApplication userApplication;

    @Autowired
    public UserAdminController(AdminServices adminServices, UserApplication userApplication) {
        this.adminServices = adminServices;
        this.userApplication = userApplication;
    }

    @Operation(
            summary = "Listar todos os usuários",
            description = "Retorna uma lista, junto com o cursor, de todos os usuários cadastrados na aplicação. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN")
    })
    @PostMapping("/search")
    public ResponseEntity<CursorPage<UserDTO>> getAll(
            @Valid @RequestBody UserSearchFilter filter,
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(adminServices.searchUsers(filter, nextCursor, pageSize));
    }

    @Operation(
            summary = "Obter dados do usuário via ID",
            description = "Retorna os detalhes do perfil do usuário usando o UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getById(@PathVariable UUID userId) {
        return userApplication.getById(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Obter dados do usuário via email",
            description = "Retorna os detalhes do perfil do usuário usando o email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil do usuário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    @GetMapping("email/{email}")
    public ResponseEntity<UserDTO> getByEmail(@PathVariable EmailAddress email) {
        return adminServices.getByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
            summary = "Bloqueia o usuário",
            description = "O perfil do usuário será bloqueado, com todas as suas credenciais suspensas e impedido de fazer login."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário bloqueado"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    @PostMapping("{userId}/block")
    public ResponseEntity<?> blockByEmail(
            @PathVariable UUID userId,
            @Valid @RequestBody BlockAccountReq req,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        adminServices.blockUser(userId, req.reason(), UUID.fromString(jwt.getSubject()));

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Desbloqueia o usuário",
            description = "O perfil do usuário que está bloqueado, será desbloqueado e retornado como ativo (ACTIVE) para a plataforma."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário bloqueado"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada / Token inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário autenticado não encontrado na base de dados")
    })
    @PostMapping("{userId}/unlock")
    public ResponseEntity<?> unlockByEmail(
            @PathVariable UUID userId,
            @Valid @RequestBody BlockAccountReq req,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        adminServices.unlockUser(userId, req.reason(), UUID.fromString(jwt.getSubject()));

        return ResponseEntity.ok().build();
    }
}
