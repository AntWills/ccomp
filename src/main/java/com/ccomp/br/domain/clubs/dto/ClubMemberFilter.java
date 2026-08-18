package com.ccomp.br.domain.clubs.dto;

import com.ccomp.br.domain.clubs.enums.ClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.ClubMemberRole;
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
    private ClubMemberRole role;
    private ClubMemberStatus status;
    private String edition;
}
