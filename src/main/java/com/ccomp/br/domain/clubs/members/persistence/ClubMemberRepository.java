package com.ccomp.br.domain.clubs.members.persistence;

import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long>, JpaSpecificationExecutor<ClubMember> {
    List<ClubMember> findByUserId(UUID userId);
    Optional<ClubMember> findByUserIdAndClubId(UUID userId, Long clubId);
    Optional<ClubMember> findByClub_IdAndUserId(Long clubId, UUID userId);
    boolean existsByUserIdAndClubIdAndRoleAndStatus(UUID userId, Long clubId, EnumClubMemberRole role, EnumClubMemberStatus status);
    boolean existsByUserIdAndClubIdAndStatus(UUID userId, Long clubId, EnumClubMemberStatus status);
    boolean existsByUserIdAndClubIdAndStatusIn(UUID userId, Long clubId,
                                               Collection<EnumClubMemberStatus> statuses);
    long countByClub_IdAndRoleAndStatus(Long clubId, EnumClubMemberRole role, EnumClubMemberStatus status);
}
