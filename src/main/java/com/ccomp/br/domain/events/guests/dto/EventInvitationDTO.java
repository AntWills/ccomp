package com.ccomp.br.domain.events.guests.dto;

import com.ccomp.br.domain.events.shared.enums.EnumInvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventInvitationDTO(
        Long id,
        UUID code,
        Long eventId,
        UUID userId,
        String emailAddress,
        EnumInvitationStatus status,
        LocalDateTime invitedAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt
) {
}
