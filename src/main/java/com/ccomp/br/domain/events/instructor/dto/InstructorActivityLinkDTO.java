package com.ccomp.br.domain.events.instructor.dto;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;

public record InstructorActivityLinkDTO(Long instructorId, Long activityId,
                                        EnumInstructorRole role, EnumInstructorStatus status) {}
