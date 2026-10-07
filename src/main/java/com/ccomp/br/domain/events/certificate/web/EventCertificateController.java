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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate (Certificados)", description = "Gestão de certificados de eventos e atividades")
public class EventCertificateController {

    private final StorageExternal storageExternal;

    public EventCertificateController(StorageExternal storageExternal) {
        this.storageExternal = storageExternal;
    }

    @PostMapping("/certificate-test")
    @Operation(summary = "Gera prévia do certificado a partir das posições enviadas pelo frontend")
    public ResponseEntity<Resource> genereteFromEvent(@Valid @RequestBody GenerateCertificatePreviewRequest request) {

        // 1. Carrega o arquivo localmente da pasta src/main/resources/templates
        Resource localTemplate = new ClassPathResource("templates/certificado-default.png");
        StorageExternal.StoredFile templateImage = new StorageExternal.StoredFile(localTemplate, "image/png");

        // 2. Mapeia os DTOs para o tipo interno Certificate.TextElement
        List<Certificate.TextElement> domainElements = request.textElements().stream()
                .map(GenerateCertificatePreviewRequest.TextElementDTO::toDomain)
                .toList();

        // Se o frontend não mandar variáveis, podemos fallbackear para variáveis padrão de teste
        Map<String, String> variables = (request.variables() != null && !request.variables().isEmpty())
                ? request.variables()
                : Map.of(
                "{participante.nome}", "José Cassios",
                "{evento.nome}", "Semana da Computação - IFMA"
        );

        // 3. Monta e gera a nova imagem em memória
        Certificate certProcessor = Certificate.builder()
                .coverImage(templateImage)
                .textElements(domainElements)
                .toReplace(variables)
                .build();

        StorageExternal.StoredFile finalCertificate = certProcessor.generate();

        // 4. Retorna a imagem processada diretamente no corpo
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(finalCertificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificado-teste.png\"")
                .body(finalCertificate.resource());
    }
}