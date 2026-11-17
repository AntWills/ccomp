package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsImageService;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "news/images (imagens)", description = "Upload, consulta e remoção da imagem de capa de uma notícia")
@RestController
@RequestMapping("api/news/{newsId}/images")
public class NewsImageController {
    private final NewsImageService imageService;

    public NewsImageController(NewsImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "Enviar ou substituir a imagem de capa da notícia")
    @ApiResponse(responseCode = "200", description = "Imagem salva", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Arquivo ausente, vazio ou tipo não suportado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadCoverImage(@PathVariable Long newsId,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        imageService.uploadCoverImage(newsId, extractUserId(jwt), file);
        return ResponseEntity.ok(new MessageResponse("Imagem de capa atualizada com sucesso."));
    }

    @Operation(summary = "Buscar a imagem de capa da notícia")
    @ApiResponse(responseCode = "200", description = "Imagem retornada", content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "204", description = "Notícia sem capa ou imagem indisponível", content = @Content)
    @SecurityRequirements
    @GetMapping("/cover")
    public ResponseEntity<Resource> getCoverImage(@PathVariable Long newsId, @AuthenticationPrincipal Jwt jwt) {
        Optional<StorageExternal.StoredFile> stored = imageService.getCoverImage(newsId, extractUserId(jwt));
        return stored.map(file -> {
            MediaType type = file.contentType() != null ? MediaType.parseMediaType(file.contentType())
                    : MediaTypeFactory.getMediaType("file").orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok().contentType(type).body(file.resource());
        }).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Remover a imagem de capa da notícia")
    @DeleteMapping("/cover")
    public ResponseEntity<?> removeCoverImage(@PathVariable Long newsId, @AuthenticationPrincipal Jwt jwt) {
        if (!imageService.removeCoverImage(newsId, extractUserId(jwt))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private @Nullable UUID extractUserId(@Nullable Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
