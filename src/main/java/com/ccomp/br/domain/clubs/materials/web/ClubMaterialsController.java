package com.ccomp.br.domain.clubs.materials.web;

import com.ccomp.br.domain.clubs.materials.application.ClubMaterialsService;
import com.ccomp.br.domain.clubs.materials.dto.CreateClubMaterialLinkRequest;
import com.ccomp.br.domain.clubs.materials.dto.ClubMaterialResponse;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}/materials")
@Tag(
        name = "clubs/materials",
        description = "Consulta e gerenciamento de links e arquivos associados a um clube.")
public class ClubMaterialsController {
    private final ClubMaterialsService service;

    public ClubMaterialsController(ClubMaterialsService service) {
        this.service = service;
    }

    @Operation(
            summary = "Lista os materiais de um clube",
            description = """
                    Retorna uma página de metadados dos materiais, sem o conteúdo dos arquivos.
                    `pageSize` é opcional, padrão 10 e limitado a 50. Use o `next_cursor` retornado como
                    `nextCursor` para buscar a próxima página, mantendo o mesmo tamanho. Exige vínculo
                    ativo com o clube; MODERATOR e ADMIN também podem acessar, qualquer que seja o status.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página retornada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CursorPage.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para visualizar o clube",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado", content = @Content)
    })
    @GetMapping
    public ResponseEntity<CursorPage<ClubMaterialResponse>> list(
            @Parameter(description = "ID numérico do clube", example = "42")
            @PathVariable Long clubId,
            @Parameter(description = "Cursor opaco retornado na página anterior")
            @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens solicitados; padrão 10 e máximo 50", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal @Nullable Jwt jwt) {
        return ResponseEntity.ok(service.list(clubId, userId(jwt), nextCursor, pageSize));
    }

    @Operation(
            summary = "Adiciona um link aos materiais do clube",
            description = """
                    Cria um material do tipo LINK. `title` é obrigatório (até 255 caracteres),
                    `description` é opcional (até 1000) e `url` deve ser HTTP ou HTTPS (até 2048).
                    Requer JWT de instrutor ativo do clube, MODERATOR ou ADMIN.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Link criado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClubMaterialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou URL não suportada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o clube",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/links")
    public ResponseEntity<ClubMaterialResponse> addLink(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubMaterialLinkRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201)
                .body(service.addLink(clubId, requireUser(jwt), request));
    }

    @Operation(
            summary = "Envia um arquivo para o clube",
            description = """
                    Recebe multipart/form-data com `title`, `file` e `description` opcional. Aceita PDF,
                    PNG, JPEG e WebP de até 10 MB; o conteúdo é verificado por assinatura. Requer JWT
                    de instrutor ativo do clube, MODERATOR ou ADMIN.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Arquivo enviado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClubMaterialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo vazio, grande ou tipo/conteúdo inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o clube",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Clube não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClubMaterialResponse> upload(
            @PathVariable Long clubId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201)
                .body(service.upload(clubId, requireUser(jwt), title, description, file));
    }

    @Operation(
            summary = "Baixa um arquivo de material",
            description = """
                    Faz streaming do arquivo depois de verificar a visibilidade do clube. Links não possuem
                    arquivo e retornam 404. Exige vínculo ativo com o clube; MODERATOR e ADMIN também
                    podem acessar, qualquer que seja o status.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo retornado",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar o clube",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Clube, material ou arquivo não encontrado",
                    content = @Content)
    })
    @GetMapping("/{materialId}/file")
    public ResponseEntity<Resource> download(
            @PathVariable Long clubId,
            @PathVariable Long materialId,
            @AuthenticationPrincipal @Nullable Jwt jwt) {
        Optional<StorageExternal.StoredFile> stored =
                service.getFile(clubId, materialId, userId(jwt));
        return stored.map(file -> {
            MediaType type = file.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(file.contentType());
            return ResponseEntity.ok()
                    .contentType(type)
                    .header("X-Content-Type-Options", "nosniff")
                    .body(file.resource());
        }).orElseGet(() -> ResponseEntity.notFound().<Resource>build());
    }

    @Operation(
            summary = "Remove um material do clube",
            description = """
                    Remove o metadado e, após a confirmação da transação, o arquivo associado ao material.
                    Requer JWT de instrutor ativo do clube, MODERATOR ou ADMIN.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Material removido"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o clube",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Clube ou material não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long clubId,
            @PathVariable Long materialId,
            @AuthenticationPrincipal Jwt jwt) {
        service.delete(clubId, materialId, requireUser(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(@Nullable Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }

    private UUID requireUser(@Nullable Jwt jwt) {
        if (jwt == null) {
            throw new UserNotFoundException("O usuário precisa estar autenticado.");
        }
        return userId(jwt);
    }
}
