package com.ccomp.br.domain.clubs.certificates.application;

import com.ccomp.br.domain.clubs.certificates.dto.ClubCertificateValidationResponse;
import com.ccomp.br.domain.clubs.certificates.enums.EnumClubCertificateType;
import com.ccomp.br.domain.clubs.certificates.persistence.ClubCertificateValidationCode;
import com.ccomp.br.domain.clubs.certificates.persistence.ClubCertificateValidationCodeRepository;
import com.ccomp.br.domain.clubs.core.persistence.Club;
import com.ccomp.br.domain.clubs.core.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.members.application.ClubAccessPolicy;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.members.persistence.ClubMemberRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.certificate.BoxElement;
import com.ccomp.br.module.certificate.Certificate;
import com.ccomp.br.module.certificate.EnumAlignment;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.dto.UserDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClubCertificateService {
    private static final String CODE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClubRepository clubRepository;
    private final ClubMemberRepository memberRepository;
    private final ClubCertificateValidationCodeRepository codeRepository;
    private final ClubCertificateWorkloadService workloadService;
    private final ClubAccessPolicy accessPolicy;
    private final UserManagement userManagement;
    private final Clock clock;
    private final String validationBaseUrl;

    public ClubCertificateService(ClubRepository clubRepository,
                                  ClubMemberRepository memberRepository,
                                  ClubCertificateValidationCodeRepository codeRepository,
                                  ClubCertificateWorkloadService workloadService,
                                  ClubAccessPolicy accessPolicy,
                                  UserManagement userManagement,
                                  Clock clock,
                                  @Value("${app.frontend.validate-certificate-url}") String validationBaseUrl) {
        this.clubRepository = clubRepository;
        this.memberRepository = memberRepository;
        this.codeRepository = codeRepository;
        this.workloadService = workloadService;
        this.accessPolicy = accessPolicy;
        this.userManagement = userManagement;
        this.clock = clock;
        this.validationBaseUrl = validationBaseUrl;
    }

    @Transactional
    public StorageExternal.StoredFile issue(Long clubId, UUID recipientId, EnumClubCertificateType type,
                                             UUID requesterId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Clube não encontrado."));
        if (!recipientId.equals(requesterId) && !accessPolicy.isManager(clubId, requesterId)) {
            throw new AccessDeniedException("Somente a própria pessoa ou gerente do clube pode emitir o certificado.");
        }

        var member = memberRepository.findByClub_IdAndUserId(clubId, recipientId)
                .filter(link -> link.getStatus() == EnumClubMemberStatus.COMPLETED)
                .filter(link -> type == EnumClubCertificateType.INSTRUCTOR
                        ? link.getRole() == EnumClubMemberRole.INSTRUCTOR
                        : link.getRole() == EnumClubMemberRole.MEMBER)
                .orElseThrow(() -> new AccessDeniedException("A pessoa precisa estar com status COMPLETED no papel correspondente para emitir o certificado."));

        UserDTO user = userManagement.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        ClubCertificateValidationCode code = codeRepository
                .findByClubIdAndUserIdAndCertificateType(clubId, recipientId, type)
                .orElseGet(() -> createCode(clubId, recipientId, type,
                        workloadService.forType(clubId, type)));

        String roleName = type == EnumClubCertificateType.INSTRUCTOR ? "instrutor" : "membro";
        String validationUrl = validationBaseUrl + (validationBaseUrl.contains("?") ? "&" : "?")
                + "code=" + code.getCode();
        Map<String, String> values = Map.of(
                "{pessoa.nome}", user.name(),
                "{clube.nome}", club.getName(),
                "{clube.papel}", roleName,
                "{certificado.carga_horaria}", formatHours(code.getWorkloadHours()),
                "{certificado.data}", DATE_FORMAT.format(LocalDate.now(clock)),
                "{validacao.codigo}", code.getCode(),
                "{validacao.url}", validationUrl);
        String text = type == EnumClubCertificateType.INSTRUCTOR
                ? "Certificamos que *{pessoa.nome}* atuou como instrutor(a) do clube *{clube.nome}*, com carga horária de {certificado.carga_horaria} horas.\n\nEmitido em {certificado.data}. Código: {validacao.codigo}. Valide em {validacao.url}."
                : "Certificamos que *{pessoa.nome}* participou como membro do clube *{clube.nome}*, com carga horária de {certificado.carga_horaria} horas.\n\nEmitido em {certificado.data}. Código: {validacao.codigo}. Valide em {validacao.url}.";

        StorageExternal.StoredFile cover = new StorageExternal.StoredFile(
                new ClassPathResource("templates/certificado-default.png"), MediaType.IMAGE_PNG_VALUE);
        return Certificate.builder()
                .coverImage(cover)
                .boxElements(List.of(new BoxElement(text, 0.12, 0.38, 0.76, 30f, "#000000", EnumAlignment.CENTERED)))
                .toReplace(values)
                .build()
                .generate();
    }

    @Transactional
    public StorageExternal.StoredFile issueForCurrentMember(Long clubId, UUID userId) {
        var member = memberRepository.findByClub_IdAndUserId(clubId, userId)
                .filter(link -> link.getStatus() == EnumClubMemberStatus.COMPLETED)
                .orElseThrow(() -> new AccessDeniedException("É necessário ter status COMPLETED no clube para emitir o certificado."));
        EnumClubCertificateType type = member.getRole() == EnumClubMemberRole.INSTRUCTOR
                ? EnumClubCertificateType.INSTRUCTOR : EnumClubCertificateType.MEMBER;
        return issue(clubId, userId, type, userId);
    }

    @Transactional(readOnly = true)
    public ClubCertificateValidationResponse validate(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ResourceNotFoundException("Certificado não encontrado para o código informado.");
        }
        String normalized = rawCode.trim().toUpperCase();
        ClubCertificateValidationCode code = codeRepository.findByCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado para o código informado."));
        Club club = clubRepository.findById(code.getClubId())
                .orElseThrow(() -> new DomainException("O clube vinculado não existe."));
        UserDTO user = userManagement.findById(code.getUserId())
                .orElseThrow(() -> new DomainException("O usuário vinculado não existe."));
        return new ClubCertificateValidationResponse(code.getCode(), club.getId(), club.getName(),
                user.id(), user.name(), code.getCertificateType(), code.getWorkloadHours(), code.getCreatedAt());
    }

    private ClubCertificateValidationCode createCode(Long clubId, UUID userId,
                                                       EnumClubCertificateType type, BigDecimal workload) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = RandomStringUtils.secure().next(CODE_LENGTH, CODE_ALPHABET);
            if (!codeRepository.existsByCode(code)) {
                return codeRepository.save(ClubCertificateValidationCode.builder()
                        .code(code).clubId(clubId).userId(userId).certificateType(type)
                        .workloadHours(workload).build());
            }
        }
        throw new DomainException("Não foi possível gerar um código de validação único.");
    }

    private String formatHours(BigDecimal hours) {
        return hours.stripTrailingZeros().toPlainString();
    }
}
