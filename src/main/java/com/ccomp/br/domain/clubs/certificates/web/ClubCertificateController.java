package com.ccomp.br.domain.clubs.certificates.web;

import com.ccomp.br.domain.clubs.certificates.application.ClubCertificateService;
import com.ccomp.br.domain.clubs.certificates.application.ClubCertificateWorkloadService;
import com.ccomp.br.domain.clubs.certificates.dto.ClubCertificateValidationResponse;
import com.ccomp.br.domain.clubs.certificates.dto.ClubCertificateWorkloadRequest;
import com.ccomp.br.domain.clubs.certificates.dto.ClubCertificateWorkloadResponse;
import com.ccomp.br.domain.clubs.certificates.enums.EnumClubCertificateType;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@Tag(name = "clubs/certificates", description = "Emissão de certificados e configuração de cargas horárias de clubes")
public class ClubCertificateController {
    private final ClubCertificateService certificateService;
    private final ClubCertificateWorkloadService workloadService;

    public ClubCertificateController(ClubCertificateService certificateService,
                                     ClubCertificateWorkloadService workloadService) {
        this.certificateService = certificateService;
        this.workloadService = workloadService;
    }

    @Operation(summary = "Emitir o próprio certificado do clube",
            description = "Exige vínculo no papel de MEMBER ou INSTRUCTOR com status COMPLETED.")
    @GetMapping(value = "/{clubId}/certificates/me", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> issueForMe(@PathVariable Long clubId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = requireUser(jwt);
        return certificateResponse(certificateService.issueForCurrentMember(clubId, userId));
    }

    @Operation(summary = "Emitir certificado de um membro",
            description = "A própria pessoa ou um gerente do clube pode emitir; o vínculo precisa estar COMPLETED com papel MEMBER.")
    @GetMapping(value = "/{clubId}/certificates/members/{recipientId}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> issueForMember(@PathVariable Long clubId, @PathVariable UUID recipientId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return certificateResponse(certificateService.issue(
                clubId, recipientId, EnumClubCertificateType.MEMBER,
                requireUser(jwt)));
    }

    @Operation(summary = "Emitir certificado de um instrutor",
            description = "O próprio instrutor ou um gerente do clube pode emitir; o vínculo precisa estar COMPLETED com papel INSTRUCTOR.")
    @GetMapping(value = "/{clubId}/certificates/instructors/{recipientId}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource> issueForInstructor(@PathVariable Long clubId, @PathVariable UUID recipientId,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return certificateResponse(certificateService.issue(
                clubId, recipientId, EnumClubCertificateType.INSTRUCTOR,
                requireUser(jwt)));
    }

    @Operation(summary = "Consultar cargas horárias dos certificados do clube")
    @GetMapping("/{clubId}/certificates/workload")
    public ResponseEntity<ClubCertificateWorkloadResponse> getWorkload(
            @PathVariable Long clubId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workloadService.get(clubId, requireUser(jwt)));
    }

    @Operation(summary = "Configurar cargas horárias dos certificados do clube")
    @PutMapping("/{clubId}/certificates/workload")
    public ResponseEntity<ClubCertificateWorkloadResponse> updateWorkload(
            @PathVariable Long clubId, @Valid @RequestBody ClubCertificateWorkloadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(workloadService.update(clubId, requireUser(jwt), request));
    }

    @Operation(summary = "Validar um certificado de clube")
    @GetMapping("/certificates/validation/{code}")
    public ResponseEntity<ClubCertificateValidationResponse> validate(
            @PathVariable String code) {
        return ResponseEntity.ok(certificateService.validate(code));
    }

    private UUID requireUser(Jwt jwt) {
        if (jwt == null) throw new UserNotFoundException("O usuário precisa estar autenticado.");
        return UUID.fromString(jwt.getSubject());
    }

    private ResponseEntity<Resource> certificateResponse(StorageExternal.StoredFile certificate) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certificate.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-clube.png\"")
                .body(certificate.resource());
    }
}
