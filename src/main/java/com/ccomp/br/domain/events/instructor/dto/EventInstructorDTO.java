package com.ccomp.br.domain.events.instructor.dto;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventInstructorDTO(
        Long id,
        UUID userId,
        Long activityId,
        EnumInstructorStatus status,
        String name,
        String description,
        EnumInstructorRole role,
        boolean isGuest,
        LocalDateTime createdAt
) {
}
