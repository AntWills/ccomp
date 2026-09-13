package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;

import java.util.Optional;

public record ClubMemberFilter (
        EnumClubMemberRole role,
        EnumClubMemberStatus status
) {
    public Optional<EnumClubMemberRole> roleOpt() {
        return Optional.ofNullable(role);
    }

    public Optional<EnumClubMemberStatus> statusOpt() {
        return Optional.ofNullable(status);
    }
}
