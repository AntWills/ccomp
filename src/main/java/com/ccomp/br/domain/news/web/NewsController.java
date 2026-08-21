package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsApplication;
import com.ccomp.br.domain.news.dto.*;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notícias", description = "Operações relacionadas ao gerenciamento de notícias.")
@RestController
@RequestMapping("api/news")
public class NewsController {
    private final NewsApplication newsApplication;

    public NewsController(NewsApplication newsApplication) {
        this.newsApplication = newsApplication;
    }

    // Rotas publicas
    @GetMapping("/{slug}")
    @Operation(
            summary = "Obtém uma notícia pelo slug",
            description = "Retorna os detalhes completos de uma notícia específica através do seu slug amigável.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notícia encontrada",
                            content = @Content(schema = @Schema(implementation = NewsResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Notícia não encontrada"
                    )
            }
    )
    @SecurityRequirements
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        return newsApplication.getBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/search")
    @Operation(
            summary = "Lista notícias com filtros",
            description = "Retorna uma lista paginada de notícias simplificadas, permitindo filtrar por destaque.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de notícias recuperada com sucesso"
                    )
            }
    )
    @SecurityRequirements
    public ResponseEntity<CursorPage<NewsItem>> searchNews(
            @Valid @RequestBody NewsFilter filter,
            @RequestParam(required = false) String nextCursor,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(
                newsApplication.searchNewsWithFilters(filter, nextCursor, pageSize)
        );
    }

    // Rotas privadas
    @Operation(
            summary = "Obtém uma notícia pelo ID (Admin)",
            description = "Retorna os detalhes de uma notícia para fins administrativos.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notícia encontrada",
                            content = @Content(schema = @Schema(implementation = NewsResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Notícia não encontrada"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado"
                    )
            }
    )
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return newsApplication.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Cria um template básico de notícia",
            description = "Cria uma nova entrada de notícia vazia vinculada ao usuário autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Rascunho criado com sucesso",
                            content = @Content(schema = @Schema(implementation = NewsResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado"
                    )
            }
    )
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt){
        NewsResponse entity = newsApplication.create(UUID.fromString(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.CREATED).body(entity);
    }

    @Operation(
            summary = "Publica uma notícia",
            description = "Define a data de publicação da notícia, tornando-a visível publicamente.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notícia publicada com sucesso"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Notícia não encontrada"
                    )
            }
    )
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        newsApplication.publish(id, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Atualiza parcialmente uma notícia",
            description = "Realiza a atualização parcial (PATCH) de uma notícia existente. " +
                    "Apenas os campos enviados no corpo da requisição serão modificados, mantendo os demais inalterados. " +
                    "Os campos permitidos na atualização incluem: título, resumo, imagem de capa, statusAccount de destaque e blocos de conteúdo. " +
                    "A requisição deve ser enviada no formato JSON através do corpo da requisição (verifique o schema NewsUpdateDto para detalhes dos campos). " +
                    "O ID da notícia alvo deve ser informado na URL. " +
                    "Requer autenticação com um token JWT válido pertencente ao autor.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notícia atualizada com sucesso",
                            content = @Content(schema = @Schema(implementation = NewsResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados inválidos",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Notícia não encontrada"
                    )
            }
    )
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody NewsUpdateDto dto,
                                    @AuthenticationPrincipal Jwt jwt) {
        NewsResponse entity = newsApplication.update(id, dto, UUID.fromString(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.OK).body(entity);
    }

    @Operation(
            summary = "Deletar uma nóticia",
            description = "Rota que permite que o autor delete qualquer nóticia que pertença a ele, mesmo que ela já tenha sido publicada.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Nóticia deletada com sucesso.",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Não autorizado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Notícia não encontrada"
                    )
            }
    )
    @DeleteMapping("/{newsId}")
    public ResponseEntity<MessageResponse> deleteById(@PathVariable Long newsId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                new MessageResponse("Nóticia deletada com sucesso.")
        );
    }
}
