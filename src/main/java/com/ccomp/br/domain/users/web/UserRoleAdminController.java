package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.security.roles.application.RolesServices;
import com.ccomp.br.domain.security.roles.enums.EnumRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Tag(name = "Gestão de Cargos (Roles)", description = "Atribuição e revogação de papéis de acesso")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserRoleAdminController {
    private final RolesServices rolesServices;

    public UserRoleAdminController(RolesServices rolesServices) {
        this.rolesServices = rolesServices;
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
}
