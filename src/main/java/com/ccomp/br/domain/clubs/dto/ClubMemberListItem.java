package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.shared.dto.UserSummaryView;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClubMemberListItem(
        Long id,
        Long clubId,
        UserSummaryView user,
        EnumClubMemberRole role,
        EnumClubMemberStatus status,
        LocalDateTime joinedAt,
        LocalDateTime leftAt
) {
}
