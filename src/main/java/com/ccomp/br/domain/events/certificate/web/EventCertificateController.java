package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.dto.GenerateCertificatePreviewRequest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.module.certificate.Certificate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate (Certificados)", description = "Prévia de certificados de eventos e atividades")
public class EventCertificateController {

    private static final long MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
    private final Resource defaultTemplate = new ClassPathResource("templates/certificado-default.png");

    @PostMapping(
            value = "/certificate-test",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = { MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(summary = "Gera prévia PNG usando o fundo e as caixas de texto enviados")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    public ResponseEntity<Resource> generatePreview(
            @Valid @RequestPart("request") GenerateCertificatePreviewRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) {

        StorageExternal.StoredFile template;
        if (coverImage != null && !coverImage.isEmpty()) {
            if (coverImage.getSize() > MAX_UPLOAD_BYTES) {
                throw new IllegalArgumentException("A imagem de fundo deve ter no máximo 10 MB.");
            }
            template = new StorageExternal.StoredFile(
                    new ByteArrayResource(readBytes(coverImage)),
                    coverImage.getContentType()
            );
        } else {
            template = new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);
        }

        Map<String, String> variables = request.variables();
        if (variables == null || variables.isEmpty()) {
            variables = Map.of(
                    "{participante.nome}", "Nome de Exemplo",
                    "{evento.nome}", "Evento de Teste",
                    "{evento.carga_horaria}", "20 horas"
            );
        }

        var elements = request.textElements().stream()
                .map(GenerateCertificatePreviewRequest.TextElementDTO::toDomain)
                .toList();

        StorageExternal.StoredFile result = Certificate.builder()
                .coverImage(template)
                .textElements(elements)
                .toReplace(variables)
                .build()
                .generate();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificado-previa.png\"")
                .body(result.resource());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.", e);
        }
    }
}