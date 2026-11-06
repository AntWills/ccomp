package com.ccomp.br.domain.events.instructor.dto;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityParticipantDTO(
        Long id,
        UUID userId,
        Long activityId,
        EnumInstructorStatus status,
        String name,
        String description,
        String role,
        boolean isGuest,
        LocalDateTime createdAt
) {}
