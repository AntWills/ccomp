package com.ccomp.br.domain.clubs.members.dto;

import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;

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
