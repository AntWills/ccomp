package com.ccomp.br.domain.events.guests.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkGuestAccountDTO(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Long activityId
) {}
