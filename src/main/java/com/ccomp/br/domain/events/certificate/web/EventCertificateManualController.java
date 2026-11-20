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
        description = "Emissão manual e envio de certificados para participantes, ministrantes, convidados e equipe, além de pré-visualização e emissão de certificados personalizados")
public class EventCertificateManualController {
    private final ManualCertificateServices manualCertificateServices;
    private final ManualParticipantCertificateServices participantCertificateServices;

    public EventCertificateManualController(ManualCertificateServices manualCertificateServices,
            ManualParticipantCertificateServices participantCertificateServices) {
        this.manualCertificateServices = manualCertificateServices;
        this.participantCertificateServices = participantCertificateServices;
    }

    @Operation(summary = "Emitir manualmente certificado de participante do evento",
            description = "Para que serve: permite à equipe emitir e baixar o certificado geral de uma inscrição do evento.\n\nComo usar: informe `eventId` e `enrollmentId`. Usa o template geral do evento. O evento deve ter terminado, não estar cancelado, e a inscrição precisa ter check-in.\n\nQuem pode usar: owner, editor ativo ou outra pessoa autorizada a editar o evento. O certificado é emitido para a pessoa dona da inscrição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou inscrição sem check-in", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão para editar o evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento ou inscrição não encontrada neste evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/enrollments/{enrollmentId}/certificate/manual",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualEventParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        var certificate = participantCertificateServices.buildEventCertificate(eventId, enrollmentId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Emitir manualmente certificado de participante da atividade",
            description = "Para que serve: permite à equipe emitir e baixar o certificado de uma inscrição para uma atividade específica.\n\nComo usar: informe `eventId`, `activityId` e `enrollmentId`. Usa o template da atividade, com fallback para o template padrão. A inscrição não pode estar cancelada e precisa ter check-in no evento e presença registrada na atividade; o evento deve ter terminado e não estar cancelado.\n\nQuem pode usar: owner, editor ativo ou outra pessoa autorizada a editar o evento. O certificado é emitido para a pessoa dona da inscrição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou conteúdo inelegível", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão ou participante sem presença exigida", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade ou inscrição não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/activities/{activityId}/enrollments/{enrollmentId}/certificate/manual",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildManualActivityParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        var certificate = participantCertificateServices.buildActivityCertificate(
                eventId, activityId, enrollmentId, extractUserId(jwt));
        return certificateResponse(certificate);
    }

    @Operation(summary = "Enviar por e-mail certificado manual de participante do evento",
            description = "Para que serve: gera o certificado geral da inscrição e envia o PNG como anexo para o e-mail da conta da pessoa inscrita.\n\nComo usar: informe `eventId` e `enrollmentId`; não há corpo na requisição. O evento deve ter terminado, não estar cancelado, e a inscrição precisa ter check-in.\n\nQuem pode usar: owner, editor ativo ou outra pessoa autorizada a editar o evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado enviado por e-mail", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou inscrição sem check-in", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão para editar o evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, inscrição ou conta do participante não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/enrollments/{enrollmentId}/certificate/manual/email")
    public ResponseEntity<MessageResponse> emailManualEventParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        participantCertificateServices.sendEventCertificateByEmail(eventId, enrollmentId, extractUserId(jwt));
        return ResponseEntity.ok(new MessageResponse("Certificado enviado por e-mail."));
    }

    @Operation(summary = "Enviar por e-mail certificado manual de participante da atividade",
            description = "Para que serve: gera o certificado da atividade e envia o PNG como anexo para o e-mail da conta da pessoa inscrita.\n\nComo usar: informe `eventId`, `activityId` e `enrollmentId`; não há corpo na requisição. A inscrição não pode estar cancelada e precisa ter check-in no evento e presença registrada na atividade; o evento deve ter terminado e não estar cancelado.\n\nQuem pode usar: owner, editor ativo ou outra pessoa autorizada a editar o evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado enviado por e-mail", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Evento ainda não encerrado, cancelado ou conteúdo inelegível", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Solicitante sem permissão ou participante sem presença exigida", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, atividade, inscrição ou conta do participante não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @PostMapping(value = "/{eventId}/activities/{activityId}/enrollments/{enrollmentId}/certificate/manual/email")
    public ResponseEntity<MessageResponse> emailManualActivityParticipantCertificate(@PathVariable Long eventId,
            @PathVariable Long activityId, @PathVariable Long enrollmentId, @AuthenticationPrincipal Jwt jwt) {
        participantCertificateServices.sendActivityCertificateByEmail(
                eventId, activityId, enrollmentId, extractUserId(jwt));
        return ResponseEntity.ok(new MessageResponse("Certificado enviado por e-mail."));
    }

    @Operation(summary = "Emitir manualmente certificado de ministrante ou convidado",
            description = "Para que serve: emite e baixa manualmente um certificado de ministrante ou de convidado apresentador.\n\nComo usar: envie um JSON com exatamente uma destas opções: `instructor_id` para certificado geral do ministrante; `instructor_id` e `activity_id` para o certificado desse vínculo na atividade; ou `activity_id` e `event_guest_id` para o convidado `PRESENTER` confirmado nessa atividade. Não combine `instructor_id` com `event_guest_id`. Use o ID `EventGuest` em `event_guest_id`, não o ID do vínculo `ActivityGuest`. O convidado não precisa ter conta associada. A emissão usa os templates configurados. Para ministrante geral com conta, a carga soma a configuração geral do evento às atividades em que a conta tem vínculo confirmado; sem conta, usa a carga geral configurada mais a atividade vinculada. Na emissão específica, considera somente a carga da atividade. Emissões manuais de ministrante/convidado não criam código público de validação.\n\nQuem pode usar: pessoas autenticadas com permissão de edição do evento. O evento deve estar encerrado e não cancelado.")
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
            description = "Para que serve: permite à equipe emitir e baixar um certificado para um integrante da equipe do evento.\n\nComo usar: informe `eventId` e `recipientId` (UUID da conta destinatária). O destinatário precisa ser owner ou editor ativo. Usa o template `EDITOR` e a carga horária configurada para a equipe; os marcadores de validação seguem as variáveis configuradas nesse template.\n\nQuem pode usar: pessoa autenticada com permissão de edição do evento. O evento deve estar encerrado e não cancelado.")
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

    @Operation(summary = "Pré-visualizar certificado personalizado",
            description = "Para que serve: mostra uma prévia do certificado usando o fundo atual do evento, sem emitir um certificado definitivo.\n\nComo usar: informe `eventId` e envie no corpo JSON as caixas e o mapa de variáveis a renderizar. A resposta é um PNG inline e não grava os dados enviados.\n\nQuem pode usar: pessoa autenticada com permissão de edição do evento. Não exige que o evento tenha terminado.")
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

    @Operation(summary = "Emitir manualmente certificado personalizado",
            description = "Para que serve: gera e baixa um certificado definitivo com o conteúdo montado pelo cliente.\n\nComo usar: informe `eventId` e envie no corpo JSON as caixas e o mapa de variáveis. A rota usa o fundo configurado para o evento, não cria código de validação automaticamente e não salva os dados recebidos. O evento deve estar encerrado e não cancelado.\n\nQuem pode usar: pessoa autenticada com permissão de edição do evento.")
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
