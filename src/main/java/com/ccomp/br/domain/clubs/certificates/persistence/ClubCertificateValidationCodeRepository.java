package com.ccomp.br.domain.clubs.certificates.persistence;

import com.ccomp.br.domain.clubs.certificates.enums.EnumClubCertificateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClubCertificateValidationCodeRepository extends JpaRepository<ClubCertificateValidationCode, Long> {
    Optional<ClubCertificateValidationCode> findByCode(String code);
    Optional<ClubCertificateValidationCode> findByClubIdAndUserIdAndCertificateType(
            Long clubId, UUID userId, EnumClubCertificateType certificateType);
    boolean existsByCode(String code);
}
