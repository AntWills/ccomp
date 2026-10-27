package com.ccomp.br.domain.events.core.web;

import com.ccomp.br.domain.events.core.application.EventImageService;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Tag(name = "events/images (imagens)", description = "Upload, consulta e remoção da imagem de capa de um evento")
@RestController
@RequestMapping("api/events/{eventId}/images")
@RequiredArgsConstructor
public class EventImageController {

    private final EventImageService eventImageService;

    @Operation(
            summary = "Enviar/substituir a imagem de capa do evento",
            description = "Envia uma imagem PNG, JPEG ou WebP como multipart/form-data. Requer JWT do proprietário, " +
                    "editor ativo, MODERATOR ou ADMIN. A capa anterior é removida do storage após o novo arquivo ser " +
                    "enviado e associado. Retorna 200 com MessageResponse."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagem salva",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente, vazio ou tipo não suportado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de edição",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadCoverImage(
            @Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @Parameter(description = "Arquivo PNG, JPEG ou WebP", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        eventImageService.uploadCoverImage(eventId, extractUserId(jwt), file);
        return ResponseEntity.ok(new MessageResponse("Imagem de capa atualizada com sucesso."));
    }

    @Operation(
            summary = "Buscar a imagem de capa do evento",
            description = "Retorna os bytes da capa com o Content-Type armazenado (PNG, JPEG ou WebP; " +
                    "application/octet-stream como fallback). Evento PUBLISHED ou UNLISTED pode ser consultado " +
                    "anonimamente. Evento DRAFT ou CANCELED requer proprietário, editor ativo, MODERATOR ou ADMIN. " +
                    "Se não houver objeto de capa disponível, retorna 204 sem corpo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso",
                    content = {
                            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary"))
                    }),
            @ApiResponse(responseCode = "204", description = "Evento sem capa associada ou objeto de imagem indisponível", content = @Content),
            @ApiResponse(responseCode = "403", description = "Evento privado e usuário sem permissão de visualização",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/cover")
    public ResponseEntity<Resource> getCoverImage(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        Optional<StorageExternal.StoredFile> stored = eventImageService.getCoverImage(eventId, extractUserId(jwt));
        return stored.map(file -> {
            MediaType mediaType = file.contentType() != null
                    ? MediaType.parseMediaType(file.contentType())
                    : MediaTypeFactory.getMediaType("file").orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok().contentType(mediaType).body(file.resource());
        }).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Remover imagem de capa do evento",
            description = "Remove a imagem de capa do storage e limpa sua associação ao evento. Requer JWT do " +
                    "proprietário, editor ativo, MODERATOR ou ADMIN. Retorna 204 sem corpo quando remove; retorna " +
                    "404 com MessageResponse quando o evento existe mas não tem capa."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Imagem removida com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de edição",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado (ErrorResponse) ou evento sem capa associada (MessageResponse)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {MessageResponse.class, ErrorResponse.class})))
    })
    @DeleteMapping("/cover")
    public ResponseEntity<?> removeCoverImage(
            @Parameter(description = "ID do evento", example = "42") @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {

        boolean removed = eventImageService.removeCoverImage(eventId, extractUserId(jwt));

        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Este evento não possui imagem de capa definida."));
        }

        return ResponseEntity.noContent().build();
    }

    private @Nullable UUID extractUserId(@Nullable Jwt jwt) {
        if(jwt == null)
            return null;

        return UUID.fromString(jwt.getSubject());
    }
}
