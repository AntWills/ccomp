package com.ccomp.br.domain.events.guests.dto;

import java.time.LocalDateTime;

public record EventInvitationCursor(
        Long id,
        LocalDateTime invitedAt
) {
}
