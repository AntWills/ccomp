package com.ccomp.br.domain.clubs.members.dto;

import java.time.LocalDateTime;

public record ClubMemberCursor(
        Long id,
        LocalDateTime joinedAt
) {}
