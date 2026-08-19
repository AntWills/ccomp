package com.ccomp.br.domain.clubs.persistence.members;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long>, JpaSpecificationExecutor<ClubMember> {
    List<ClubMember> findByUserId(UUID userId);
    Optional<ClubMember> findByUserIdAndClubId(UUID userId, Long clubId);
    boolean existsByUserIdAndClubIdAndRoleAndStatus(UUID userId, Long clubId, EnumClubMemberRole role, EnumClubMemberStatus status);
}