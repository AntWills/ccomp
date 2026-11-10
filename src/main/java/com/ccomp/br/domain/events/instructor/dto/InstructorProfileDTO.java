package com.ccomp.br.domain.events.instructor.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InstructorProfileDTO(Long id, Long eventId, UUID userId, String name,
                                   String description, LocalDateTime createdAt,
                                   List<InstructorActivityLinkDTO> activities) {}
