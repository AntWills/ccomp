package com.ccomp.br.domain.events.instructor.dto;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorInvitationStatus;

import java.time.LocalDateTime;

public record InstructorInvitationDTO(
        Long id,
        Long activityId,
        String emailAddress,
        EnumInstructorInvitationStatus status,
        LocalDateTime invitedAt,
        LocalDateTime expiresAt,
        LocalDateTime respondedAt
) {
}
