package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.issuance.AutomaticCertificateServices;
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
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/events")
@Tag(name = "events/certificate/issuance (Emissão de certificados)",
        description = "Emissão de certificados para participantes, ministrantes e equipe do evento")
public class EventCertificateIssuanceController {
    private final AutomaticCertificateServices automaticCertificateServices;

    public EventCertificateIssuanceController(AutomaticCertificateServices automaticCertificateServices) {
        this.automaticCertificateServices = automaticCertificateServices;
    }

    @Operation(summary = "Consultar certificado do evento",
            description = "Gera o certificado do evento para o usuário autenticado somente após o encerramento do evento. Exige inscrição e status `CHECKED_IN` no evento. Eventos cancelados não emitem certificado. Credenciamento operacional não é requisito; a carga horária considera as atividades em que a pessoa participou. O parâmetro opcional `name` personaliza o nome impresso sem alterar a conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, evento cancelado ou participante sem check-in no evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem acesso ao evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromEvent(@PathVariable Long eventId,
                                                               @RequestParam(required = false) @Size(max = 255) String name,
                                                               @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = automaticCertificateServices.buildCertificateFromEvent(
                eventId, extractUserId(jwt), name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-evento-%d.png\"".formatted(eventId))
                .body(certificate.resource());
    }

    @Operation(summary = "Consultar certificado de atividade",
            description = "Gera o certificado de atividade para o usuário autenticado. Exige inscrição ativa, status `CHECKED_IN` no evento e presença registrada especificamente nesta atividade. Após confirmar a presença pelo QR Code, a plataforma atualiza o check-in do evento e envia um e-mail avisando que o certificado está disponível. Credenciamento operacional é independente e não substitui os registros de presença. O parâmetro opcional `name` personaliza o nome impresso sem alterar a conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Check-in do evento ou participação na atividade não registrados", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não autenticado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada ou usuário não inscrito", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/activities/{activityId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromActivity(@PathVariable Long activityId,
                                                                  @RequestParam(required = false) @Size(max = 255) String name,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = automaticCertificateServices.buildCertificateFromActivity(
                activityId, extractUserId(jwt), name);
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
        StorageExternal.StoredFile certificate = automaticCertificateServices.buildCertificateFromInstructor(
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
        StorageExternal.StoredFile certificate = automaticCertificateServices.buildCertificateFromActivityGuest(
                eventId, activityId, activityGuestId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-ministrante-convidado-%d.png".formatted(activityGuestId));
    }

    @GetMapping(value = "/{eventId}/activities/{activityId}/instructors/{instructorId}/activity-certificate",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildActivityCertificateFromInstructor(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long instructorId, @AuthenticationPrincipal Jwt jwt) {
        var certificate = automaticCertificateServices.buildActivityCertificateFromInstructor(
                eventId, activityId, instructorId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-ministrante-atividade-%d.png".formatted(instructorId));
    }

    @GetMapping(value = "/{eventId}/activities/{activityId}/guests/{activityGuestId}/activity-certificate",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildActivityCertificateFromGuest(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long activityGuestId, @AuthenticationPrincipal Jwt jwt) {
        var certificate = automaticCertificateServices.buildActivityCertificateFromGuest(
                eventId, activityId, activityGuestId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-convidado-atividade-%d.png".formatted(activityGuestId));
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
        StorageExternal.StoredFile certificate = automaticCertificateServices.buildCertificateForEditor(
                eventId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-equipe-%d.png".formatted(eventId));
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
