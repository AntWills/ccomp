package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ClubMemberFilter {
    private Long clubId;
    private UUID userId;
    private EnumClubMemberRole role;
    private EnumClubMemberStatus status;
    private String edition;
}
