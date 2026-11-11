package com.ccomp.br.domain.events.instructor.dto;

import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;

public record LinkInstructorProfileRequest(EnumInstructorRole role) {}
