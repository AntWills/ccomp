package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.CertificateCoverService;
import com.ccomp.br.domain.events.certificate.application.CertificateBoxServices;
import com.ccomp.br.domain.events.certificate.application.CertificateServices;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.dto.GenerateCertificatePreviewRequest;
import com.ccomp.br.domain.events.certificate.dto.SaveCertificateBoxesRequest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.module.certificate.Certificate;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate (Certificados)",
        description = "Prévia de certificados e gestão do fundo (cover template) do certificado do evento")
public class EventCertificateController {
    private final CertificateCoverService certificateCoverService;
    private final CertificateBoxServices certificateBoxServices;
    private final CertificateServices certificateServices;

    public EventCertificateController(CertificateCoverService certificateCoverService,
                                      CertificateBoxServices certificateBoxServices,
                                      CertificateServices certificateServices) {
        this.certificateCoverService = certificateCoverService;
        this.certificateBoxServices = certificateBoxServices;
        this.certificateServices = certificateServices;
    }

    @Operation(
            summary = "Gerar certificado do evento",
            description = "Gera o certificado em PNG usando o fundo, as caixas e as variáveis configuradas para o evento. " +
                    "Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromEvent(eventId, extractUserId(jwt));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificado-evento-%d.png\"".formatted(eventId))
                .body(certificate.resource());
    }

    @Operation(
            summary = "Gera prévia PNG do certificado",
            description = "Renderiza a prévia usando o fundo do evento (ou o padrão) e as caixas de " +
                    "texto enviadas no JSON. Se `variables` não for enviado, usa valores de exemplo. " +
                    "Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prévia PNG gerada",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Dados da prévia inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(
            value = "/{eventId}/certificate-test",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = { MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public ResponseEntity<Resource> generatePreview(
            @Valid @RequestBody GenerateCertificatePreviewRequest request,
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        var template = certificateCoverService.loadCoverTemplate(eventId, extractUserId(jwt));

        Map<String, String> variables = request.variables();
        if (variables == null || variables.isEmpty()) {
            variables = Map.of(
                    "{participante.nome}", "Nome de Exemplo",
                    "{evento.nome}", "Evento de Teste",
                    "{evento.carga_horaria}", "20 horas"
            );
        }

        var elements = request.boxElements().stream()
                .map(BoxElementDTO::toDomain)
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

    @Operation(
            summary = "Enviar/substituir o fundo (cover template) do certificado",
            description = "Envia o arquivo diretamente (multipart/form-data). Se já existir um fundo, " +
                    "ele é substituído e o anterior é removido do storage. Aceita PNG, JPEG ou WebP. " +
                    "Requer permissão de edição do evento (dono, editor ativo ou moderador/admin)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fundo do certificado atualizado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente ou tipo não suportado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário não é dono nem editor do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(
            value = "/{eventId}/certificate-cover-template",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MessageResponse> uploadCoverTemplate(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        certificateCoverService.uploadCoverTemplate(eventId, extractUserId(jwt), file);
        return ResponseEntity.ok(new MessageResponse("Fundo do certificado atualizado com sucesso."));
    }

    @Operation(
            summary = "Salvar as caixas do template do certificado",
            description = "Cria ou substitui as caixas de texto persistidas para o evento. Aceita até 30 caixas. " +
                    "Requer permissão de edição do evento (dono, editor ativo ou moderador/admin)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template de caixas salvo com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lista vazia ou dados inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de edição do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/certificate-box-template", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> saveBoxTemplate(
            @PathVariable Long eventId,
            @Valid @RequestBody SaveCertificateBoxesRequest request,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        certificateBoxServices.saveBoxTemplate(eventId, extractUserId(jwt), request.boxes());
        return ResponseEntity.ok(new MessageResponse("Template de caixas salvo com sucesso."));
    }

    private @Nullable UUID extractUserId(@Nullable Jwt jwt) {
        if (jwt == null)
            return null;

        return UUID.fromString(jwt.getSubject());
    }
}
