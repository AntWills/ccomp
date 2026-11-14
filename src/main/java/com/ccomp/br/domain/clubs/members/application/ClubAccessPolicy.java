package com.ccomp.br.domain.clubs.members.application;

import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.clubs.members.persistence.ClubMemberRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClubAccessPolicy {
    private final ClubMemberRepository clubMemberRepository;

    public ClubAccessPolicy(ClubMemberRepository clubMemberRepository) {
        this.clubMemberRepository = clubMemberRepository;
    }

    public boolean isInstructor(Long clubId, UUID userId) {
        return userId != null && clubMemberRepository.existsByUserIdAndClubIdAndRoleAndStatus(userId, clubId,
                EnumClubMemberRole.INSTRUCTOR,
                EnumClubMemberStatus.ACTIVE);
    }

    public boolean isActiveMember(Long clubId, UUID userId) {
        return userId != null && clubMemberRepository.existsByUserIdAndClubIdAndStatus(
                userId, clubId, EnumClubMemberStatus.ACTIVE);
    }

    public boolean isManager(Long clubId, UUID userId) {
        return SecurityUtils.isModeratorOrAdmin() || isInstructor(clubId, userId);
    }
}
