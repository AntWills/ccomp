package com.ccomp.br.domain.events.activities.materials.web;

import com.ccomp.br.domain.events.activities.materials.application.ActivityMaterialsService;
import com.ccomp.br.domain.events.materials.dto.CreateMaterialLinkRequest;
import com.ccomp.br.domain.events.materials.dto.MaterialResponse;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import com.ccomp.br.shared.utils.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/activities/{activityId}/materials")
@Tag(name = "events/activities/materials", description = "Consulta e gerenciamento de links e arquivos associados a uma atividade.")
public class ActivityMaterialsController {
    private final ActivityMaterialsService service;
    public ActivityMaterialsController(ActivityMaterialsService service) { this.service = service; }

    @Operation(summary = "Lista os materiais de uma atividade", description = "Retorna uma página de metadados dos materiais, sem o conteúdo dos arquivos. `pageSize` é opcional, padrão 10 e limitado a 50. Use o `next_cursor` retornado como `nextCursor` para buscar a próxima página, mantendo o mesmo tamanho. A visibilidade segue a do evento ao qual a atividade pertence; eventos publicados são visíveis publicamente e os demais exigem proprietário, editor ativo, MODERATOR ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página retornada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CursorPage.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar o evento", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento não encontrado", content = @Content)
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<CursorPage<MaterialResponse>> list(
            @Parameter(description = "ID numérico da atividade", example = "123") @PathVariable Long activityId,
            @Parameter(description = "Cursor opaco retornado na página anterior") @RequestParam(required = false) String nextCursor,
            @Parameter(description = "Itens solicitados; padrão 10 e máximo 50", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal @Nullable Jwt jwt) {
        return ResponseEntity.ok(service.list(activityId, userId(jwt), nextCursor, pageSize));
    }
    @Operation(summary = "Adiciona um link aos materiais da atividade", description = "Cria um material do tipo LINK. `title` é obrigatório (até 255 caracteres), `description` é opcional (até 1000) e `url` deve ser HTTP ou HTTPS (até 2048). Requer JWT do proprietário, de um editor ativo do evento, MODERATOR ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Link criado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MaterialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou URL não suportada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o evento", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/links")
    public ResponseEntity<MaterialResponse> addLink(@PathVariable Long activityId, @Valid @RequestBody CreateMaterialLinkRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(service.addLink(activityId, requireUser(jwt), request));
    }
    @Operation(summary = "Envia um arquivo para a atividade", description = "Recebe multipart/form-data com `title`, `file` e `description` opcional. Aceita PDF, PNG, JPEG e WebP de até 10 MB; o conteúdo é verificado por assinatura. Requer JWT do proprietário, de um editor ativo do evento, MODERATOR ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Arquivo enviado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MaterialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo vazio, acima do limite ou tipo/conteúdo inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o evento", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade ou evento não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> upload(@PathVariable Long activityId, @RequestParam String title,
            @RequestParam(required = false) String description, @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(service.upload(activityId, requireUser(jwt), title, description, file));
    }
    @Operation(summary = "Baixa um arquivo de material", description = "Faz streaming do arquivo depois de verificar a visibilidade do evento relacionado. Links não possuem arquivo e retornam 404.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo retornado", content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar o evento", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade, evento, material ou arquivo não encontrado", content = @Content)
    })
    @SecurityRequirements
    @GetMapping("/{materialId}/file")
    public ResponseEntity<Resource> download(@PathVariable Long activityId, @PathVariable Long materialId, @AuthenticationPrincipal @Nullable Jwt jwt) {
        Optional<StorageExternal.StoredFile> stored = service.getFile(activityId, materialId, userId(jwt));
        return stored.map(file -> {
            MediaType type = file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(file.contentType());
            return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options", "nosniff").body(file.resource());
        }).orElseGet(() -> ResponseEntity.notFound().<Resource>build());
    }
    @Operation(summary = "Remove um material da atividade", description = "Remove o metadado e, após a confirmação da transação, o arquivo associado ao material. Requer JWT do proprietário, de um editor ativo do evento, MODERATOR ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Material removido"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar o evento", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade, evento ou material não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> delete(@PathVariable Long activityId, @PathVariable Long materialId, @AuthenticationPrincipal Jwt jwt) {
        service.delete(activityId, materialId, requireUser(jwt)); return ResponseEntity.noContent().build();
    }
    private UUID userId(@Nullable Jwt jwt) { return jwt == null ? null : UUID.fromString(jwt.getSubject()); }
    private UUID requireUser(@Nullable Jwt jwt) { if (jwt == null) throw new com.ccomp.br.shared.exceptions.UserNotFoundException("O usuário precisa estar autenticado."); return userId(jwt); }
}
