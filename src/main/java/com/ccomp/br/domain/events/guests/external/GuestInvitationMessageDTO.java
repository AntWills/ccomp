package com.ccomp.br.domain.events.guests.external;

import com.ccomp.br.module.email.EmailAddress;

import java.util.UUID;

public record GuestInvitationMessageDTO(
        Long eventId,
        String eventTitle,
        UUID code,
        EmailAddress emailAddress
) {
}
