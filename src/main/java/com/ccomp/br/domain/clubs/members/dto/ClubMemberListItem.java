package com.ccomp.br.domain.clubs.members.dto;

import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClubMemberListItem(
        UserSummaryDTO user,
        Long id,
        Long clubId,
        EnumClubMemberRole role,
        EnumClubMemberStatus status,
        LocalDateTime joinedAt,
        LocalDateTime leftAt
) {
}
