package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.issuance.ManualCertificateServices;
import com.ccomp.br.domain.events.certificate.dto.ManualInstructorCertificateRequest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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

    public EventCertificateManualController(ManualCertificateServices manualCertificateServices) {
        this.manualCertificateServices = manualCertificateServices;
    }

    @Operation(summary = "Emitir manualmente certificado de ministrante",
            description = "A equipe emite um certificado individual para um ministrante cadastrado ou para um convidado confirmado com papel PRESENTER. Não exige conta para o convidado. Emissão manual não gera código de validação; carga horária usa a atividade indicada ou a soma das atividades do ministrante vinculado.")
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
            @Valid @RequestBody ManualInstructorCertificateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        StorageExternal.StoredFile certificate = manualCertificateServices.buildInstructorCertificate(
                eventId, request, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-ministrante-manual.png");
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
        StorageExternal.StoredFile certificate = manualCertificateServices.buildTeamCertificate(
                eventId, recipientId, extractUserId(jwt));
        return certificateResponse(certificate, "certificado-equipe-%s.png".formatted(recipientId));
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
