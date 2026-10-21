package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.CertificateServices;
import com.ccomp.br.domain.events.certificate.application.CertificateValidatorService;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import com.ccomp.br.domain.storage.external.StorageExternal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate/consultation (Consulta de certificados)",
        description = "Emissão de certificados para participantes e validação pública de certificados")
public class EventCertificateConsultationController {
    private final CertificateServices certificateServices;
    private final CertificateValidatorService certificateValidatorService;

    public EventCertificateConsultationController(CertificateServices certificateServices,
                                                  CertificateValidatorService certificateValidatorService) {
        this.certificateServices = certificateServices;
        this.certificateValidatorService = certificateValidatorService;
    }

    @Operation(summary = "Consultar certificado do evento",
            description = "Gera e retorna o certificado do evento para o usuário autenticado. Requer acesso de visualização do evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuário sem acesso ao evento", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/{eventId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromEvent(@PathVariable Long eventId,
                                                               @AuthenticationPrincipal @Nullable Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromEvent(eventId, extractUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-evento-%d.png\"".formatted(eventId))
                .body(certificate.resource());
    }

    @Operation(summary = "Consultar certificado de atividade",
            description = "Gera o certificado de atividade do usuário autenticado. Requer inscrição na atividade e check-in realizado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado PNG gerado", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Check-in não realizado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não autenticado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada ou usuário não inscrito", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/activities/{activityId}/certificate", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> buildCertificateFromActivity(@PathVariable Long activityId,
                                                                  @AuthenticationPrincipal @Nullable Jwt jwt) {
        StorageExternal.StoredFile certificate = certificateServices.buildCertificateFromActivity(activityId, extractUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-atividade-%d.png\"".formatted(activityId))
                .body(certificate.resource());
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

    private @Nullable UUID extractUserId(@Nullable Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
