package com.ccomp.br.domain.clubs.members.persistence;

import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long>, JpaSpecificationExecutor<ClubMember> {
    List<ClubMember> findByUserId(UUID userId);
    Optional<ClubMember> findByUserIdAndClubId(UUID userId, Long clubId);
    boolean existsByUserIdAndClubIdAndRoleAndStatus(UUID userId, Long clubId, EnumClubMemberRole role, EnumClubMemberStatus status);
    boolean existsByUserIdAndClubIdAndStatus(UUID userId, Long clubId, EnumClubMemberStatus status);
}
