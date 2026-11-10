package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.*;
import com.ccomp.br.domain.events.certificate.application.boxes.ActivityCertificateBoxServices;
import com.ccomp.br.domain.events.certificate.application.boxes.CertificateBoxServices;
import com.ccomp.br.domain.events.certificate.application.variables.CertificateVariablesServices;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.dto.BoxTemplateResponse;
import com.ccomp.br.domain.events.certificate.dto.SaveCertificateBoxesRequest;
import com.ccomp.br.domain.events.certificate.enums.EnumCertificateVariableContext;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.events.certificate.enums.EnumEventCertificateTemplateType;
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
@Tag(name = "events/certificate/management (Gestão de certificados)",
        description = "Consulta e edição dos templates e fundos de certificados por gestores e editores do evento")
public class EventCertificateManagementController {
    private final CertificateCoverService certificateCoverService;
    private final CertificateBoxServices certificateBoxServices;
    private final ActivityCertificateBoxServices activityCertificateBoxServices;

    public EventCertificateManagementController(CertificateCoverService certificateCoverService,
                                      CertificateBoxServices certificateBoxServices,
                                      ActivityCertificateBoxServices activityCertificateBoxServices) {
        this.certificateCoverService = certificateCoverService;
        this.certificateBoxServices = certificateBoxServices;
        this.activityCertificateBoxServices = activityCertificateBoxServices;
    }

    @Operation(
            summary = "Consultar template do certificado do evento",
            description = "Retorna as caixas de texto e as variáveis em uso no certificado do evento. " +
                    "O parâmetro `type` seleciona PARTICIPANT, INSTRUCTOR ou EDITOR; se não houver template salvo, retorna o padrão daquele tipo. " +
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
            @RequestParam(defaultValue = "PARTICIPANT") EnumEventCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var template = type == EnumEventCertificateTemplateType.PARTICIPANT
                ? certificateBoxServices.loadBoxTemplate(eventId, extractUserId(jwt))
                : certificateBoxServices.loadBoxTemplate(eventId, extractUserId(jwt), type);
        return ResponseEntity.ok(new BoxTemplateResponse(template.getBoxes(), template.getVariables()));
    }

    @Operation(
            summary = "Consultar template do certificado de atividade",
            description = "Retorna as caixas e variáveis do template de fallback do evento para o contexto selecionado. " +
                    "Se ainda não houver template salvo para esse contexto, retorna o padrão. " +
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
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var userId = extractUserId(jwt);
        var template = type == EnumActivityCertificateTemplateType.PARTICIPANT
                ? activityCertificateBoxServices.loadBoxTemplate(eventId, userId)
                : activityCertificateBoxServices.loadBoxTemplate(eventId, userId, type);
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
            @AuthenticationPrincipal Jwt jwt
    ) {
        StorageExternal.StoredFile cover = certificateCoverService.loadCoverTemplate(eventId, extractUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .body(cover.resource());
    }

    @Operation(
            summary = "Gerar prévia do certificado do evento",
            description = "Renderiza o fundo compartilhado e as caixas do tipo selecionado (PARTICIPANT, INSTRUCTOR, GUEST ou EDITOR), preenchendo as variáveis com dados de exemplo. " +
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
            @RequestParam(defaultValue = "PARTICIPANT") EnumEventCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        var cover = certificateCoverService.loadCoverTemplate(eventId, userId);
        var template = type == EnumEventCertificateTemplateType.PARTICIPANT
                ? certificateBoxServices.loadBoxTemplate(eventId, userId)
                : certificateBoxServices.loadBoxTemplate(eventId, userId, type);
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
            @RequestParam(required = false) Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);
        if (activityId == null) {
            var cover = certificateCoverService.loadCoverTemplate(eventId, userId);
            var template = type == EnumActivityCertificateTemplateType.PARTICIPANT
                    ? activityCertificateBoxServices.loadBoxTemplate(eventId, userId)
                    : activityCertificateBoxServices.loadBoxTemplate(eventId, userId, type);
            return renderPreview(cover, template.getBoxes(), mockCertificateVariables());
        }
        var template = activityCertificateBoxServices.loadActivityTemplate(eventId, activityId, userId, type);
        return renderPreview(template.cover(), template.boxes(), mockCertificateVariables());
    }

    @Operation(
            summary = "Enviar/substituir o fundo (cover template) do certificado",
            description = "Envia o arquivo diretamente (multipart/form-data). Se já existir um fundo, " +
                    "ele é substituído e o anterior é removido do storage após salvar a alteração. Valida o conteúdo " +
                    "da imagem e aceita PNG, JPEG ou WebP com até 30 milhões de pixels. " +
                    "Requer permissão de edição do evento (dono, editor ativo ou moderador/admin)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fundo do certificado atualizado com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente, conteúdo inválido, formato não suportado ou resolução acima do limite",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário não é owner, editor ativo, moderador ou admin",
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
            @AuthenticationPrincipal Jwt jwt
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
            @ApiResponse(responseCode = "403", description = "Usuário não é owner, editor ativo, moderador ou admin",
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
            @AuthenticationPrincipal Jwt jwt
    ) {
        boolean removed = certificateCoverService.deleteCoverTemplate(eventId, extractUserId(jwt));

        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Este evento não possui um fundo de certificado definido."));
        }

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Listar variáveis suportadas por tipo de certificado",
            description = "Retorna somente as variáveis válidas para o contexto selecionado: PARTICIPANT, ACTIVITY, INSTRUCTOR, GUEST, ACTIVITY_INSTRUCTOR, ACTIVITY_GUEST ou EDITOR. Se omitido, usa PARTICIPANT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de variáveis para o contexto informado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Contexto de certificado inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/certificate-variables", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listCertificateVariables(
            @RequestParam(defaultValue = "PARTICIPANT") EnumCertificateVariableContext type
    ) {
        return ResponseEntity.ok(CertificateVariablesServices.variablesFor(type));
    }

    @Operation(
            summary = "Salvar template do evento",
            description = "Cria ou substitui as caixas do tipo selecionado pelo parâmetro `type` (PARTICIPANT, INSTRUCTOR, GUEST ou EDITOR). Aceita até 30 caixas. " +
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
            @RequestParam(defaultValue = "PARTICIPANT") EnumEventCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (type == EnumEventCertificateTemplateType.PARTICIPANT) {
            certificateBoxServices.saveBoxTemplate(eventId, extractUserId(jwt), request.boxes());
        } else {
            certificateBoxServices.saveBoxTemplate(eventId, extractUserId(jwt), request.boxes(), type);
        }
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
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (type == EnumActivityCertificateTemplateType.PARTICIPANT)
            activityCertificateBoxServices.saveBoxTemplate(eventId, extractUserId(jwt), request.boxes());
        else
            activityCertificateBoxServices.saveBoxTemplate(eventId, extractUserId(jwt), request.boxes(), type);
        return ResponseEntity.ok(new MessageResponse("Template de caixas do certificado de atividade salvo com sucesso."));
    }

    @GetMapping(value = "/{eventId}/activities/{activityId}/certificate-box-template",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BoxTemplateResponse> getSpecificActivityBoxTemplate(
            @PathVariable Long eventId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt) {
        var template = activityCertificateBoxServices.loadActivityTemplate(
                eventId, activityId, extractUserId(jwt), type);
        return ResponseEntity.ok(new BoxTemplateResponse(template.boxes(), template.variables()));
    }

    @PostMapping(value = "/{eventId}/activities/{activityId}/certificate-box-template",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> saveSpecificActivityBoxTemplate(
            @PathVariable Long eventId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @Valid @RequestBody SaveCertificateBoxesRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        activityCertificateBoxServices.saveActivityTemplate(eventId, activityId, extractUserId(jwt),
                request.boxes(), type);
        return ResponseEntity.ok(new MessageResponse("Template específico da atividade salvo com sucesso."));
    }

    @DeleteMapping(value = "/{eventId}/activities/{activityId}/certificate-box-template")
    public ResponseEntity<?> deleteSpecificActivityBoxTemplate(
            @PathVariable Long eventId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt) {
        boolean removed = certificateCoverService.deleteActivityCertificateTemplate(
                eventId, activityId, extractUserId(jwt), type);
        return removed ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("A atividade não possui template específico para este contexto."));
    }

    @GetMapping(value = "/{eventId}/activities/{activityId}/certificate-cover-template",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp"})
    public ResponseEntity<Resource> getSpecificActivityCoverTemplate(
            @PathVariable Long eventId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt) {
        var resolved = activityCertificateBoxServices.loadActivityTemplate(
                eventId, activityId, extractUserId(jwt), type);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(resolved.cover().contentType()))
                .body(resolved.cover().resource());
    }

    @PostMapping(value = "/{eventId}/activities/{activityId}/certificate-cover-template",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadSpecificActivityCoverTemplate(
            @PathVariable Long eventId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        certificateCoverService.uploadActivityCoverTemplate(eventId, activityId, extractUserId(jwt), type, file);
        return ResponseEntity.ok(new MessageResponse("Fundo específico da atividade atualizado com sucesso."));
    }

    @DeleteMapping(value = "/{eventId}/activities/{activityId}/certificate-cover-template")
    public ResponseEntity<?> deleteSpecificActivityCoverTemplate(
            @PathVariable Long eventId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "PARTICIPANT") EnumActivityCertificateTemplateType type,
            @AuthenticationPrincipal Jwt jwt) {
        boolean removed = certificateCoverService.deleteActivityCoverTemplate(
                eventId, activityId, extractUserId(jwt), type);
        return removed ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("A atividade não possui um fundo específico deste contexto."));
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
                Map.entry("{ministrante.nome}", "Nome de Exemplo"),
                Map.entry("{convidado.nome}", "Convidado de Exemplo"),
                Map.entry("{equipe.nome}", "Nome de Exemplo"),
                Map.entry("{evento.title}", "Evento de Exemplo"),
                Map.entry("{evento.data_inicio}", "08/11/2026"),
                Map.entry("{evento.data_fim}", "09/11/2026"),
                Map.entry("{evento.local}", "Auditório de Exemplo"),
                Map.entry("{evento.carga_horaria}", "20"),
                Map.entry("{ministrante.carga_horaria}", "12"),
                Map.entry("{convidado.carga_horaria}", "10"),
                Map.entry("{equipe.carga_horaria}", "8"),
                Map.entry("{atividade.nome}", "Atividade de Exemplo"),
                Map.entry("{atividade.local}", "Sala de Exemplo"),
                Map.entry("{atividade.carga_horaria}", "4"),
                Map.entry("{evento.organizacao}", "Organização de Exemplo"),
                Map.entry("{validacao.codigo}", "ABC123"),
                Map.entry("{validacao.url}", "https://exemplo.invalid/validar/ABC123")
        );
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
