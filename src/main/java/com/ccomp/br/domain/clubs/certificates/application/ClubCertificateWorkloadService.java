package com.ccomp.br.domain.clubs.certificates.application;

import com.ccomp.br.domain.clubs.certificates.dto.ClubCertificateWorkloadRequest;
import com.ccomp.br.domain.clubs.certificates.dto.ClubCertificateWorkloadResponse;
import com.ccomp.br.domain.clubs.certificates.enums.EnumClubCertificateType;
import com.ccomp.br.domain.clubs.certificates.persistence.ClubCertificateSettings;
import com.ccomp.br.domain.clubs.certificates.persistence.ClubCertificateSettingsRepository;
import com.ccomp.br.domain.clubs.core.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.members.application.ClubAccessPolicy;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ClubCertificateWorkloadService {
    private final ClubRepository clubRepository;
    private final ClubCertificateSettingsRepository settingsRepository;
    private final ClubAccessPolicy accessPolicy;

    public ClubCertificateWorkloadService(ClubRepository clubRepository,
                                          ClubCertificateSettingsRepository settingsRepository,
                                          ClubAccessPolicy accessPolicy) {
        this.clubRepository = clubRepository;
        this.settingsRepository = settingsRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public ClubCertificateWorkloadResponse get(Long clubId, UUID requesterId) {
        ensureClubExists(clubId);
        ensureManager(clubId, requesterId);
        return response(clubId, settingsRepository.findById(clubId).orElse(null));
    }

    @Transactional
    public ClubCertificateWorkloadResponse update(Long clubId, UUID requesterId,
                                                   ClubCertificateWorkloadRequest request) {
        ensureClubExists(clubId);
        ensureManager(clubId, requesterId);
        ClubCertificateSettings settings = settingsRepository.findById(clubId)
                .orElseGet(() -> ClubCertificateSettings.builder().clubId(clubId).build());
        settings.setMemberWorkloadHours(request.memberWorkloadHours());
        settings.setInstructorWorkloadHours(request.instructorWorkloadHours());
        return response(clubId, settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public BigDecimal forType(Long clubId, EnumClubCertificateType type) {
        ClubCertificateSettings settings = settingsRepository.findById(clubId).orElse(null);
        if (settings == null) return BigDecimal.ZERO;
        return type == EnumClubCertificateType.INSTRUCTOR
                ? settings.getInstructorWorkloadHours() : settings.getMemberWorkloadHours();
    }

    private ClubCertificateWorkloadResponse response(Long clubId, ClubCertificateSettings settings) {
        return new ClubCertificateWorkloadResponse(clubId,
                settings == null ? BigDecimal.ZERO : settings.getMemberWorkloadHours(),
                settings == null ? BigDecimal.ZERO : settings.getInstructorWorkloadHours());
    }

    private void ensureClubExists(Long clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ResourceNotFoundException("Clube não encontrado.");
        }
    }

    private void ensureManager(Long clubId, UUID requesterId) {
        if (!accessPolicy.isManager(clubId, requesterId)) {
            throw new AccessDeniedException("Somente gerentes do clube podem configurar a carga horária.");
        }
    }
}
