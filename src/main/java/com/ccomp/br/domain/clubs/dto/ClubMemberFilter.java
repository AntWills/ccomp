package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;

public record ClubMemberFilter (
        EnumClubMemberRole role,
        EnumClubMemberStatus status
) {
}
