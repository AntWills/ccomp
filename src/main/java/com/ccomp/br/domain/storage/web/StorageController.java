package com.ccomp.br.domain.storage.web;

import com.ccomp.br.domain.storage.application.StorageService;
import com.ccomp.br.domain.storage.dto.UploadFileResponse;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Storage", description = "Gerenciamento e manipulação de arquivos no S3/MinIO")
@RestController
@RequestMapping("api/storage")
@RequiredArgsConstructor
public class StorageController {
    private final StorageService storageService;

    @Operation(
            summary = "Realizar upload de arquivo",
            description = "Envia um arquivo para o bucket S3/MinIO e retorna a URL pública e o nome gerado com UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UploadFileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo não fornecido ou inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer perfil ADMIN ou STAFF)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno ao salvar o arquivo no storage", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadFileResponse> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(storageService.upload(file));
    }

    @Operation(
            summary = "Buscar/Baixar arquivo pelo nome",
            description = "Recupera o recurso armazenado no bucket e o retorna com o Content-Type apropriado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo retornado com sucesso",
                    content = @Content(mediaType = MediaType.ALL_VALUE)),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer perfil ADMIN ou STAFF)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Arquivo solicitado não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/{fileName}")
    public ResponseEntity<?> getImage(@PathVariable String fileName) {
        Optional<Resource> resource = storageService.findByFileName(fileName);

        return resource.<ResponseEntity<?>>map(res -> {
            // Detecta o tipo da imagem automaticamente (PNG, JPEG, WebP, etc.)
            MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(res);
        }).orElseGet(() ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Arquivo solicitado não encontrado."))
        );
    }

    @Operation(
            summary = "Remover arquivo do storage",
            description = "Deleta permanentemente o arquivo especificado do bucket."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Arquivo excluído com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer perfil ADMIN ou STAFF)", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileName) {
        storageService.delete(fileName);
        return ResponseEntity.noContent().build();
    }
}
