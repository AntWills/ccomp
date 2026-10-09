package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.dto.GenerateCertificatePreviewRequest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.module.certificate.Certificate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate (Certificados)", description = "Prévia de certificados de eventos e atividades")
public class EventCertificateController {

    private final Resource defaultTemplate = new ClassPathResource("templates/certificado-default.png");

    @PostMapping(
            value = "/certificate-test",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.IMAGE_PNG_VALUE
    )
    @Operation(summary = "Gera prévia PNG usando o fundo padrão e as caixas de texto enviadas no JSON")
    public ResponseEntity<Resource> generatePreview(
            @Valid @RequestBody GenerateCertificatePreviewRequest request
    ) {
        StorageExternal.StoredFile template = new StorageExternal.StoredFile(defaultTemplate, MediaType.IMAGE_PNG_VALUE);

        Map<String, String> variables = request.variables();
        if (variables == null || variables.isEmpty()) {
            variables = Map.of(
                    "{participante.nome}", "Nome de Exemplo",
                    "{evento.nome}", "Evento de Teste",
                    "{evento.carga_horaria}", "20 horas"
            );
        }

        var elements = request.textElements().stream()
                .map(GenerateCertificatePreviewRequest.BoxElementDTO::toDomain)
                .toList();

        StorageExternal.StoredFile result = Certificate.builder()
                .coverImage(template)
                .boxElements(elements)
                .toReplace(variables)
                .build()
                .generate();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificado-previa.png\"")
                .body(result.resource());
    }
}