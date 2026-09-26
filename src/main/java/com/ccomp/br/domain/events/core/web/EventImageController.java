package com.ccomp.br.domain.events.core.web;

import com.ccomp.br.domain.events.core.application.EventImageService;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
            description = "Envia o arquivo diretamente (multipart/form-data). Se já existir uma capa, ela é " +
                    "substituída e a anterior é removida do storage. Aceita PNG, JPEG ou WebP."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Imagem enviada e associada com sucesso", content = @Content),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente ou tipo não suportado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário não é dono nem editor do evento", content = @Content),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content)
    })
    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadCoverImage(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        eventImageService.uploadCoverImage(eventId, extractUserId(jwt), file);
        return ResponseEntity.ok(new MessageResponse("Imavem de fundo atualizada com sucesso."));
    }

    @Operation(
            summary = "Buscar a imagem de capa do evento",
            description = "Retorna os bytes da imagem diretamente. Eventos públicos ou não listados podem " +
                    "ser acessados sem autenticação."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso",
                    content = @Content(mediaType = MediaType.ALL_VALUE)),
            @ApiResponse(responseCode = "204", description = "Evento ainda não possui imagem de capa", content = @Content),
            @ApiResponse(responseCode = "403", description = "Evento privado e usuário sem permissão de visualização", content = @Content),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content)
    })
    @GetMapping("/cover")
    public ResponseEntity<Resource> getCoverImage(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Chega na API");
        Optional<StorageExternal.StoredFile> stored = eventImageService.getCoverImage(eventId, extractUserId(jwt));
        log.info("Busca no Storage");
        return stored.map(file -> {
            MediaType mediaType = file.contentType() != null
                    ? MediaType.parseMediaType(file.contentType())
                    : MediaTypeFactory.getMediaType("file").orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok().contentType(mediaType).body(file.resource());
        }).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Remover imagem de capa do evento",
            description = "Remove definitivamente a imagem de capa do storage e desassocia do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Imagem removida com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário não é dono nem editor do evento", content = @Content),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado ou sem imagem de capa definida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @DeleteMapping("/cover")
    public ResponseEntity<?> removeCoverImage(
            @PathVariable Long eventId,
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