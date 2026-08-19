package com.ccomp.br.domain.clubs.application;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMemberRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClubAccessPolicy {
    private final ClubMemberRepository clubMemberRepository;

    public ClubAccessPolicy(ClubMemberRepository clubMemberRepository) {
        this.clubMemberRepository = clubMemberRepository;
    }

    public boolean isInstructor(Long clubId, UUID userId) {
        return clubMemberRepository.existsByUserIdAndClubIdAndRoleAndStatus(userId, clubId,
                EnumClubMemberRole.INSTRUCTOR,
                EnumClubMemberStatus.ACTIVE);
    }
}
