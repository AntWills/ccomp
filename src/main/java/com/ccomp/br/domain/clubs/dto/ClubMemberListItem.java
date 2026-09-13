package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.ccomp.br.shared.dto.UserSummaryView;
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
