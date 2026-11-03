package com.ccomp.br.domain.events.guests.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LinkGuestAccountDTO(@NotNull UUID userId, @NotNull Long activityId) {}
