package com.ccomp.br.domain.events.guests.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conta ativa que será vinculada ao convidado e atividade onde poderá atuar")
public record LinkGuestAccountDTO(
        @Schema(description = "E-mail da conta ativa", example = "ana@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "ID da atividade que receberá o vínculo", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long activityId
) {}
