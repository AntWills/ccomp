package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.issuance.ManualCertificateServices;
import com.ccomp.br.domain.events.certificate.application.issuance.ManualParticipantCertificateServices;
import com.ccomp.br.domain.events.certificate.application.GeneratedCertificate;
import com.ccomp.br.domain.events.certificate.dto.ManualCertificateContentRequest;
import com.ccomp.br.domain.events.certificate.dto.ManualInstructorCertificateRequest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate/manual (Emissão manual de certificados)",
        description = "Emissão manual de certificados individuais para ministrantes e integrantes da equipe")
public class EventCertificateManualController {
    private final ManualCertificateServices manualCertificateServices;
    private final ManualParticipantCertificateServices participantCertificateServices;

    public EventCertificateManualController(ManualCertificateServices manualCertificateServices,
            ManualParticipantCertificateServices participantCertificateServices) {
        this.manualCertificateServices = manualCertificateServices;
        this.participantCertificateServices = participantCertificateServices;
    }

    @Operation(summary = "Emitir certificado manual de participante do evento")
    @PostMapping(value = "/{eventId}/enrollments/{enrollmentId}/certificate/manual",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualEventParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        var certificate = participantCertificateServices.buildEventCertificate(eventId, enrollmentId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Emitir certificado manual de participante da atividade")
    @PostMapping(value = "/{eventId}/activities/{activityId}/enrollments/{enrollmentId}/certificate/manual",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualActivityParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        var certificate = participantCertificateServices.buildActivityCertificate(
                eventId, activityId, enrollmentId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Enviar por e-mail certificado manual de participante do evento")
    @PostMapping(value = "/{eventId}/enrollments/{enrollmentId}/certificate/manual/email")
    public ResponseEntity<MessageResponse> emailManualEventParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        participantCertificateServices.sendEventCertificateByEmail(eventId, enrollmentId, extractUserId(jwt));
        return ResponseEntity.ok(new MessageResponse("Certificado enviado por e-mail."));
    }

    @Operation(summary = "Enviar por e-mail certificado manual de participante da atividade")
    @PostMapping(value = "/{eventId}/activities/{activityId}/enrollments/{enrollmentId}/certificate/manual/email")
    public ResponseEntity<MessageResponse> emailManualActivityParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        participantCertificateServices.sendActivityCertificateByEmail(
                eventId, activityId, enrollmentId, extractUserId(jwt));
        return ResponseEntity.ok(new MessageResponse("Certificado enviado por e-mail."));
    }

    @Operation(summary = "Emitir manualmente certificado de ministrante",
            description = "Envie exatamente uma opção: instructor_id sozinho emite certificado geral do ministrante; instructor_id com activity_id emite o certificado desse ministrante para a atividade vinculada; activity_id com event_guest_id emite certificado do convidado PRESENTER confirmado para essa atividade. Não combine instructor_id e event_guest_id. Convidado não precisa de conta vinculada. Emissão manual não cria código público: os marcadores ficam como 'Emissão manual' e URL vazia. A emissão específica usa somente a carga da atividade; a geral do ministrante com conta soma a carga geral configurada no evento às cargas das atividades com vínculo confirmado dessa conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Identificador inválido, evento ainda não encerrado ou cancelado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante não pertence à equipe ou vínculo do destinatário não está confirmado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade, ministrante ou convidado não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/instructor-certificate/manual",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualInstructorCertificate(
            @PathVariable Long eventId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Use exatamente um dos exemplos: ministrante geral, ministrante por atividade ou convidado por atividade.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ManualInstructorCertificateRequest.class),
                            examples = {
                                    @ExampleObject(name = "Certificado geral de ministrante", value = "{\"instructor_id\":4}"),
                                    @ExampleObject(name = "Certificado de ministrante por atividade", value = "{\"instructor_id\":4,\"activity_id\":27}"),
                                    @ExampleObject(name = "Certificado de convidado por atividade", value = "{\"activity_id\":27,\"event_guest_id\":20}")
                            }))
            @Valid @RequestBody ManualInstructorCertificateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = manualCertificateServices.buildInstructorCertificate(
                eventId, request, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Emitir manualmente certificado de integrante da equipe",
            description = "Owner, editor ativo, moderator ou admin emite um certificado individual para o owner ou um editor ativo. Usa o template EDITOR, carga horária configurada para a equipe e código de validação associado ao destinatário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou sem carga horária de equipe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão ou destinatário fora da equipe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou conta de destino não encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/team-certificate/manual/{recipientId}",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualTeamCertificate(
            @PathVariable Long eventId,
            @PathVariable UUID recipientId,
            @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = manualCertificateServices.buildTeamCertificate(
                eventId, recipientId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Pré-visualizar certificado manual personalizado",
            description = "Renderiza o fundo configurado para o evento com caixas e variáveis fornecidas pelo cliente. Não grava nem emite um certificado definitivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prévia PNG gerada",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Conteúdo do certificado inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão de edição do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/certificate/manual/preview",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> previewCustomCertificate(
            @PathVariable Long eventId,
            @Valid @RequestBody ManualCertificateContentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate preview = manualCertificateServices.previewCustomCertificate(
                eventId, extractUserId(jwt), request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(preview.file().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"%s\"".formatted(preview.fileName()))
                .body(preview.file().resource());
    }

    @Operation(summary = "Emitir certificado manual personalizado",
            description = "Gera o PNG definitivo usando as caixas e variáveis fornecidas pelo cliente. Exige permissão de edição e evento encerrado. Não cria código de validação nem salva os dados recebidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado ou conteúdo inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão de edição do evento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/certificate/manual",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> issueCustomCertificate(
            @PathVariable Long eventId,
            @Valid @RequestBody ManualCertificateContentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = manualCertificateServices.issueCustomCertificate(
                eventId, extractUserId(jwt), request);
        return certificateResponse(certificate);
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private ResponseEntity<Resource> certificateResponse(GeneratedCertificate certificate) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.file().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(certificate.fileName()))
                .body(certificate.file().resource());
    }
}
