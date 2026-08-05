package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsEditorServices;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notícias")
@RestController
@RequestMapping("api/news")
public class EditorController {
    private final NewsEditorServices newsEditorServices;

    public EditorController(NewsEditorServices newsEditorServices) {
        this.newsEditorServices = newsEditorServices;
    }

    @Operation(
            summary = "Listar editores de uma notícia",
            description = "Retorna a lista de usuários cadastrados como editores da notícia. Apenas o autor da notícia pode visualizar essa lista."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de editores obtida com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (o usuário logado não é o autor da notícia)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notícia não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{newsId}/editors")
    public ResponseEntity<List<UserDTO>> getEditors(
            @PathVariable Long newsId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(newsEditorServices.listEditors(UUID.fromString(jwt.getSubject()), newsId));
    }

    @Operation(
            summary = "Adicionar editor a uma notícia",
            description = "Concede acesso de edição de uma notícia a outro usuário através de seu e-mail. Apenas o autor da notícia pode adicionar editores."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Editor adicionado com sucesso",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Formato de e-mail inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (o usuário logado não é o autor da notícia)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notícia ou usuário não encontrado para o e-mail informado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{newsId}/editors/{email}")
    public ResponseEntity<MessageResponse> addEditor(
            @PathVariable Long newsId,
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt) {
        newsEditorServices.addEditor(UUID.fromString(jwt.getSubject()), new EmailAddress(email), newsId);
        return ResponseEntity.ok(new MessageResponse("Editor adicionado com sucesso."));
    }

    @Operation(
            summary = "Remover editor de uma notícia",
            description = "Revoga a permissão de edição de um usuário em uma notícia específica. Apenas o autor da notícia pode remover editores."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Editor removido com sucesso",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado (o usuário logado não é o autor da notícia)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notícia, usuário ou vínculo de editor não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{newsId}/editors/{email}")
    public ResponseEntity<MessageResponse> removeEditor(
            @PathVariable Long newsId,
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt) {
        newsEditorServices.removeEditor(UUID.fromString(jwt.getSubject()), new EmailAddress(email), newsId);
        return ResponseEntity.ok(new MessageResponse("Editor removido com sucesso."));
    }
}
