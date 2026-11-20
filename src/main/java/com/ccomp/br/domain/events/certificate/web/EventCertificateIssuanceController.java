package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.issuance.AutomaticCertificateServices;
import com.ccomp.br.domain.events.certificate.application.GeneratedCertificate;
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
        description = "Geração e download de certificados para participantes, ministrantes, convidados e equipe. As respostas bem-sucedidas são arquivos PNG.")
public class EventCertificateIssuanceController {
    private final AutomaticCertificateServices automaticCertificateServices;

    public EventCertificateIssuanceController(AutomaticCertificateServices automaticCertificateServices) {
        this.automaticCertificateServices = automaticCertificateServices;
    }

    @Operation(summary = "Gerar certificado de participante do evento",
            description = "Para que serve: gera e baixa o certificado geral do evento para a própria pessoa autenticada.\n\nComo usar: informe `eventId` no caminho. Opcionalmente, envie `name` (até 255 caracteres) para personalizar o nome impresso; isso não altera o nome da conta. O arquivo PNG é baixado. Exige que o evento tenha terminado, não esteja cancelado e que a pessoa inscrita tenha feito check-in no evento. A carga horária considera a carga geral do evento e as atividades em que houve participação, conforme configuração. Credenciamento operacional não substitui esses registros.\n\nQuem pode usar: participante autenticado com acesso ao evento e inscrição elegível.")
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
        GeneratedCertificate certificate = automaticCertificateServices.buildCertificateFromEvent(
                eventId, extractUserId(jwt), name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.file().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(certificate.fileName()))
                .body(certificate.file().resource());
    }

    @Operation(summary = "Gerar certificado de participante da atividade",
            description = "Para que serve: gera e baixa o certificado da atividade indicada para a própria pessoa autenticada.\n\nComo usar: informe `activityId` no caminho. Opcionalmente, envie `name` (até 255 caracteres) para personalizar o nome impresso, sem alterar a conta. O arquivo PNG é baixado. Exige inscrição não cancelada, check-in no evento e presença registrada especificamente na atividade. Esta rota apenas consulta esses registros e gera o certificado; não registra presença nem envia e-mail. Credenciamento operacional não substitui os registros de presença.\n\nQuem pode usar: participante autenticado que satisfaça os requisitos de inscrição e presença.")
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
        GeneratedCertificate certificate = automaticCertificateServices.buildCertificateFromActivity(
                activityId, extractUserId(jwt), name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.file().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(certificate.fileName()))
                .body(certificate.file().resource());
    }

    @Operation(summary = "Gerar certificado de ministrante",
            description = "Para que serve: gera e baixa o certificado geral de um ministrante vinculado ao evento.\n\nComo usar: informe `eventId` e `instructorId`. O vínculo deve estar confirmado e associado a uma conta. O certificado usa o template de ministrante do evento e o fundo compartilhado; a carga horária segue as regras do vínculo e do evento.\n\nQuem pode usar: o próprio ministrante ou uma pessoa com permissão de edição do evento (owner, editor ativo, moderator ou admin). O evento precisa estar encerrado e não cancelado.")
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
        GeneratedCertificate certificate = automaticCertificateServices.buildCertificateFromInstructor(
                eventId, instructorId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Gerar certificado geral de convidado apresentador",
            description = "Para que serve: gera e baixa o certificado geral do evento para um convidado apresentador associado a uma atividade.\n\nComo usar: informe `eventId`, `activityId` e `eventGuestId` (ID do perfil do convidado no evento, não do vínculo ActivityGuest). O convidado precisa estar confirmado e ter papel `PRESENTER` na atividade, além de conta vinculada. Usa o template geral de convidado do evento.\n\nQuem pode usar: o próprio convidado vinculado ou a equipe com permissão de edição do evento. O evento precisa estar encerrado e não cancelado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado ou cancelado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Convidado sem conta vinculada, papel incorreto ou solicitante sem permissão", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade ou vínculo de convidado não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/activities/{activityId}/guests/{eventGuestId}/instructor-certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromActivityGuest(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long eventGuestId,
            @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = automaticCertificateServices.buildCertificateFromActivityGuest(
                eventId, activityId, eventGuestId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Gerar certificado de ministrante da atividade",
            description = "Para que serve: gera e baixa o certificado de um ministrante para uma atividade específica.\n\nComo usar: informe `eventId`, `activityId` e `instructorId`. O vínculo do ministrante deve pertencer à atividade e estar confirmado. Usa o template específico da atividade quando disponível, com fallback para o template padrão; a carga horária considera somente essa atividade.\n\nQuem pode usar: o próprio ministrante associado ou a equipe com permissão de edição do evento. A elegibilidade do certificado também exige que o evento não esteja cancelado e tenha terminado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Solicitante não é o ministrante nem membro autorizado da equipe", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade ou vínculo de ministrante não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/activities/{activityId}/instructors/{instructorId}/activity-certificate",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildActivityCertificateFromInstructor(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long instructorId, @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = automaticCertificateServices.buildActivityCertificateFromInstructor(
                eventId, activityId, instructorId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Gerar certificado de convidado da atividade",
            description = "Para que serve: gera e baixa o certificado de um convidado apresentador para uma atividade específica.\n\nComo usar: informe `eventId`, `activityId` e `eventGuestId` (ID do perfil do convidado no evento, não do vínculo ActivityGuest). O perfil deve estar confirmado, ter conta vinculada e estar associado à atividade com papel `PRESENTER`. Usa o template específico da atividade quando disponível, com fallback para o template padrão; a carga horária considera somente essa atividade.\n\nQuem pode usar: o próprio convidado vinculado ou a equipe com permissão de edição do evento. A elegibilidade também exige evento encerrado e não cancelado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Convidado não é apresentador confirmado com conta vinculada, ou solicitante sem permissão", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade ou vínculo de convidado não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/activities/{activityId}/guests/{eventGuestId}/activity-certificate",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildActivityCertificateFromGuest(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long eventGuestId, @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = automaticCertificateServices.buildActivityCertificateFromGuest(
                eventId, activityId, eventGuestId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Gerar certificado da equipe do evento",
            description = "Para que serve: gera e baixa o certificado de equipe da própria pessoa autenticada.\n\nComo usar: informe `eventId`. Usa o template de equipe, o fundo compartilhado e a carga horária de equipe configurada no evento. O evento precisa ter terminado e não pode estar cancelado.\n\nQuem pode usar: owner ou editor ativo do evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou carga horária da equipe não configurada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não é owner nem editor ativo do evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/team-certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateForEditor(@PathVariable Long eventId,
                                                               @AuthenticationPrincipal Jwt jwt) {
        GeneratedCertificate certificate = automaticCertificateServices.buildCertificateForEditor(
                eventId, extractUserId(jwt));
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
