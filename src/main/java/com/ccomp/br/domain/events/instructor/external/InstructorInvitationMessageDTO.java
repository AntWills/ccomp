package com.ccomp.br.domain.events.instructor.external;

import com.ccomp.br.module.email.EmailAddress;

import java.util.UUID;

public record InstructorInvitationMessageDTO(
        Long eventId,
        String eventTitle,
        Long activityId,
        String activityTitle,
        UUID code,
        EmailAddress emailAddress
) {
}
