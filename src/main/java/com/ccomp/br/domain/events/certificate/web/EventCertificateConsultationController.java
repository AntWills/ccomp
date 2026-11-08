package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.CertificateServices;
import com.ccomp.br.domain.events.certificate.application.CertificateValidatorService;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import com.ccomp.br.domain.events.certificate.dto.ManualInstructorCertificateRequest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate/consultation (Consulta de certificados)",
        description = "Emissão de certificados para participantes, ministrantes e equipe, além da validação pública")
public class EventCertificateConsultationController {
    private final CertificateServices certificateServices;
    private final CertificateValidatorService certificateValidatorService;

    public EventCertificateConsultationController(CertificateServices certificateServices,
                                                  CertificateValidatorService certificateValidatorService) {
        this.certificateServices = certificateServices;
        this.certificateValidatorService = certificateValidatorService;
    }

    @Operation(summary = "Consultar certificado do evento",
            description = "Gera o certificado do evento para o usuário autenticado somente após o encerramento do evento. Exige inscrição e status `CHECKED_IN` no evento. Eventos cancelados não emitem certificado. Credenciamento operacional não é requisito; a carga horária considera as atividades em que a pessoa participou.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, evento cancelado ou participante sem check-in no evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem acesso ao evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromEvent(@PathVariable Long eventId,
                                                               @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromEvent(eventId, extractUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-evento-%d.png\"".formatted(eventId))
                .body(certificate.resource());
    }

    @Operation(summary = "Consultar certificado de atividade",
            description = "Gera o certificado de atividade para o usuário autenticado. Exige inscrição ativa, status `CHECKED_IN` no evento e presença registrada especificamente nesta atividade. Após confirmar a presença pelo QR Code, a plataforma atualiza o check-in do evento e envia um e-mail avisando que o certificado está disponível. Credenciamento operacional é independente e não substitui os registros de presença.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Check-in do evento ou participação na atividade não registrados", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não autenticado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada ou usuário não inscrito", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/activities/{activityId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromActivity(@PathVariable Long activityId,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromActivity(activityId, extractUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-atividade-%d.png\"".formatted(activityId))
                .body(certificate.resource());
    }

    @Operation(summary = "Gerar certificado de ministrante",
            description = "Gera o certificado do ministrante vinculado e com conta associada. O próprio ministrante ou owner, editor ativo, moderator ou admin pode solicitá-lo. Exige evento encerrado e não cancelado; usa template de ministrante e o fundo compartilhado do evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado ou cancelado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão ou conta sem vínculo de ministrante confirmado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, ministrante ou conta vinculada não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/instructors/{instructorId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromInstructor(@PathVariable Long eventId,
                                                                    @PathVariable Long instructorId,
                                                                    @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromInstructor(
                eventId, instructorId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-ministrante-%d.png".formatted(instructorId));
    }

    @Operation(summary = "Gerar certificado de convidado apresentador",
            description = "Gera o certificado individual para um convidado vinculado à atividade com papel PRESENTER. O próprio convidado pode emitir se tiver conta associada; a equipe do evento também pode solicitá-lo. Exige convidado confirmado, evento encerrado e não cancelado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado ou cancelado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Convidado sem conta vinculada, papel incorreto ou solicitante sem permissão", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade ou vínculo de convidado não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/activities/{activityId}/guests/{activityGuestId}/instructor-certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromActivityGuest(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long activityGuestId,
            @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromActivityGuest(
                eventId, activityId, activityGuestId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-ministrante-convidado-%d.png".formatted(activityGuestId));
    }

    @Operation(summary = "Emitir manualmente certificado de ministrante",
            description = "A equipe emite um certificado individual para um ministrante cadastrado ou para um convidado confirmado com papel PRESENTER na atividade. Não exige conta para o convidado. Emissão manual não gera código de validação; carga horária usa a atividade indicada ou a soma das atividades do ministrante vinculado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Identificador inválido, evento ainda não encerrado ou cancelado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante não pertence à equipe ou vínculo do destinatário não está confirmado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade, ministrante ou convidado não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/instructor-certificate/manual", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualInstructorCertificate(@PathVariable Long eventId,
            @Valid @RequestBody ManualInstructorCertificateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildManualInstructorCertificate(
                eventId, request, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-ministrante-manual.png");
    }

    @Operation(summary = "Gerar certificado da equipe do evento",
            description = "Gera o certificado do owner ou editor ativo autenticado após o encerramento do evento. Eventos cancelados não emitem certificado. Usa template próprio de equipe, fundo compartilhado e a carga horária da equipe configurada no evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou carga horária da equipe não configurada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não é owner nem editor ativo do evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/team-certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateForEditor(@PathVariable Long eventId,
                                                               @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateForEditor(
                eventId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-equipe-%d.png".formatted(eventId));
    }

    @Operation(summary = "Emitir manualmente certificado de integrante da equipe",
            description = "Owner, editor ativo, moderator ou admin emite um certificado individual para o owner ou um editor ativo. Usa o template EDITOR, carga horária configurada para a equipe e código de validação associado ao destinatário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou sem carga horária de equipe", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão ou destinatário fora da equipe", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou conta de destino não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/team-certificate/manual/{recipientId}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualTeamCertificate(@PathVariable Long eventId,
            @PathVariable UUID recipientId,
            @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateForEditor(
                eventId, recipientId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-equipe-%s.png".formatted(recipientId));
    }

    @Operation(summary = "Validar certificado pelo código",
            description = "Rota pública. Retorna os dados associados ao código impresso no certificado ou 404 se ele não existir.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado válido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CertificateValidationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Código de validação não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/certificates/validate/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CertificateValidationResponse> validateCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificateValidatorService.validate(code));
    }

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private ResponseEntity<Resource> certificateResponse(StorageExternal.StoredFile certificate, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(fileName))
                .body(certificate.resource());
    }
}
