package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.*;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.dto.BoxTemplateResponse;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate (Certificados)",
        description = "Geração e prévia de certificados do evento e de atividades, além da gestão dos templates")
public class EventCertificateController {
    private final CertificateCoverService certificateCoverService;
    private final CertificateBoxServices certificateBoxServices;
    private final CertificateServices certificateServices;
    private final ActivityCertificateBoxServices activityCertificateBoxServices;
    private final CertificateValidatorService certificateValidatorService;

    public EventCertificateController(CertificateCoverService certificateCoverService,
                                      CertificateBoxServices certificateBoxServices,
                                      CertificateServices certificateServices,
                                      ActivityCertificateBoxServices activityCertificateBoxServices,
                                      CertificateValidatorService certificateValidatorService) {
        this.certificateCoverService = certificateCoverService;
        this.certificateBoxServices = certificateBoxServices;
        this.certificateServices = certificateServices;
        this.activityCertificateBoxServices = activityCertificateBoxServices;
        this.certificateValidatorService = certificateValidatorService;
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
            summary = "Gerar certificado de atividade",
            description = "Gera o certificado em PNG da atividade para o usuário autenticado, usando o fundo do evento " +
                    "e o template de caixas compartilhado por suas atividades. " +
                    "A autorização é a própria inscrição na atividade com check-in realizado, " +
                    "não sendo necessária permissão de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Check-in na atividade não realizado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada ou usuário não inscrito",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/activities/{activityId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromActivity(
            @PathVariable Long activityId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        StorageExternal.StoredFile certificate =
                certificateServices.buildCertificateFromActivity(activityId, extractUserId(jwt));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificado-atividade-%d.png\"".formatted(activityId))
                .body(certificate.resource());
    }

    @Operation(
            summary = "Consultar template do certificado do evento",
            description = "Retorna as caixas de texto e as variáveis em uso no certificado do evento. " +
                    "Se ainda não houver template salvo, retorna o template padrão. " +
                    "Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template retornado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BoxTemplateResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate-box-template", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BoxTemplateResponse> getBoxTemplate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        var template = certificateBoxServices.loadBoxTemplate(eventId, extractUserId(jwt));
        return ResponseEntity.ok(new BoxTemplateResponse(template.getBoxes(), template.getVariables()));
    }

    @Operation(
            summary = "Consultar template do certificado de atividade",
            description = "Retorna as caixas de texto e as variáveis do template único compartilhado por todas as " +
                    "atividades do evento. Se ainda não houver template salvo, retorna o template padrão. " +
                    "Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template retornado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BoxTemplateResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/activity-certificate-box-template", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BoxTemplateResponse> getActivityBoxTemplate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        var template = activityCertificateBoxServices.loadBoxTemplate(eventId, extractUserId(jwt));
        return ResponseEntity.ok(new BoxTemplateResponse(template.getBoxes(), template.getVariables()));
    }

    @Operation(
            summary = "Consultar o fundo (cover) do certificado em uso",
            description = "Retorna os bytes da imagem de fundo atualmente em uso pelo certificado do evento. " +
                    "Se não houver fundo salvo, retorna a imagem padrão. " +
                    "Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagem de fundo retornada com sucesso",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate-cover-template",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> getCoverTemplate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        StorageExternal.StoredFile cover = certificateCoverService.loadCoverTemplate(eventId, extractUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .body(cover.resource());
    }

    @Operation(
            summary = "Gerar prévia do certificado do evento",
            description = "Renderiza o cover e as caixas salvas para o evento, preenchendo as variáveis com dados de exemplo. " +
                    "Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prévia PNG do certificado do evento gerada",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate-preview",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> previewEventCertificate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        var cover = certificateCoverService.loadCoverTemplate(eventId, userId);
        var template = certificateBoxServices.loadBoxTemplate(eventId, userId);
        return renderPreview(cover, template.getBoxes(), mockCertificateVariables());
    }

    @Operation(
            summary = "Gerar prévia do certificado de atividade",
            description = "Renderiza o cover do evento e o template de caixas compartilhado por suas atividades, " +
                    "preenchendo as variáveis com dados de exemplo. Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prévia PNG do certificado de atividade gerada",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/activity-certificate-preview",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> previewActivityCertificate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        var cover = certificateCoverService.loadCoverTemplate(eventId, userId);
        var template = activityCertificateBoxServices.loadBoxTemplate(eventId, userId);
        return renderPreview(cover, template.getBoxes(), mockCertificateVariables());
    }

    @Operation(
            summary = "Validar certificado pelo código",
            description = "Rota pública. Recebe o código de validação impresso no certificado (variável " +
                    "{validacao.codigo}) e devolve os dados correspondentes, ou 404 se o código não existir."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificado válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CertificateValidationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Código não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/certificates/validate/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CertificateValidationResponse> validateCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificateValidatorService.validate(code));
    }

    private ResponseEntity<Resource> renderPreview(StorageExternal.StoredFile cover,
                                                   List<BoxElementDTO> boxes,
                                                   Map<String, String> variables) {
        StorageExternal.StoredFile result = Certificate.builder()
                .coverImage(cover)
                .boxElements(boxes.stream().map(BoxElementDTO::toDomain).toList())
                .toReplace(variables)
                .build()
                .generate();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificado-previa.png\"")
                .body(result.resource());
    }

    private Map<String, String> mockCertificateVariables() {
        return Map.ofEntries(
                Map.entry("{participante.nome}", "Nome de Exemplo"),
                Map.entry("{evento.title}", "Evento de Exemplo"),
                Map.entry("{evento.local}", "Auditório de Exemplo"),
                Map.entry("{evento.carga_horaria}", "20 horas"),
                Map.entry("{atividade.nome}", "Atividade de Exemplo"),
                Map.entry("{atividade.local}", "Sala de Exemplo"),
                Map.entry("{atividade.carga_horaria}", "4 horas"),
                Map.entry("{evento.organizacao}", "Organização de Exemplo"),
                Map.entry("{validacao.codigo}", "ABC123"),
                Map.entry("{validacao.url}", "https://exemplo.invalid/validar/ABC123")
        );
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
            summary = "Remover o fundo (cover) do certificado",
            description = "Remove o fundo salvo do certificado do evento. Após a remoção volta a ser usada a " +
                    "imagem padrão. Requer permissão de edição do evento (dono, editor ativo ou moderador/admin)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fundo removido com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário não é dono nem editor do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado ou sem fundo definido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @DeleteMapping(value = "/{eventId}/certificate-cover-template",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteCoverTemplate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        boolean removed = certificateCoverService.deleteCoverTemplate(eventId, extractUserId(jwt));

        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Este evento não possui um fundo de certificado definido."));
        }

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Consultar variaveis dos certificados",
            description = "Retorna o template de caixas compartilhado por todas as atividades do evento. " +
                    "Se ainda não houver configuração salva, retorna o template padrão. Requer acesso de visualização do evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de variaveis suportadas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de visualização do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/certificate-variables", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> loadActivityBoxTemplate() {
        return ResponseEntity.ok(CertificateVariablesServices.VARIABLES);
    }

    @Operation(
            summary = "Salvar template do evento",
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

    @Operation(
            summary = "Salvar template da atividade",
            description = "Cria ou substitui o template de caixas único do evento, utilizado em todas as atividades. " +
                    "Aceita até 30 caixas e requer permissão de edição do evento (dono, editor ativo ou moderador/admin)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template de certificado de atividade salvo",
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
    @PostMapping(value = "/{eventId}/activity-certificate-box-template", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> saveActivityBoxTemplate(
            @PathVariable Long eventId,
            @Valid @RequestBody SaveCertificateBoxesRequest request,
            @AuthenticationPrincipal @Nullable Jwt jwt
    ) {
        activityCertificateBoxServices.saveBoxTemplate(eventId, extractUserId(jwt), request.boxes());
        return ResponseEntity.ok(new MessageResponse("Template de caixas do certificado de atividade salvo com sucesso."));
    }

    private @Nullable UUID extractUserId(@Nullable Jwt jwt) {
        if (jwt == null)
            return null;

        return UUID.fromString(jwt.getSubject());
    }
}
