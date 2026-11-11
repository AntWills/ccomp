package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.CertificateValidatorService;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@Tag(name = "events/certificate/validation (Validação de certificados)",
        description = "Validação pública de certificados emitidos")
public class EventCertificateValidationController {
    private final CertificateValidatorService certificateValidatorService;

    public EventCertificateValidationController(CertificateValidatorService certificateValidatorService) {
        this.certificateValidatorService = certificateValidatorService;
    }

    @Operation(summary = "Validar certificado pelo código",
            description = "Rota pública. Retorna os dados associados ao código impresso no certificado ou 404 se ele não existir.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CertificateValidationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Código de validação não encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.ccomp.br.shared.exceptions.ErrorResponse.class)))
    })
    @GetMapping(value = "/certificates/validate/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CertificateValidationResponse> validateCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificateValidatorService.validate(code));
    }
}
