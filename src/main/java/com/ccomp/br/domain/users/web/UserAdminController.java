package com.ccomp.br.domain.users.web;

import com.ccomp.br.domain.users.application.AdminServices;
import com.ccomp.br.domain.users.application.UserApplication;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
            description = "Retorna uma lista com todos os usuários cadastrados na aplicação. Requer permissão ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Requisição não autenticada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — Requer perfil ADMIN")
    })
    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userApplication.getAll());
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
}
