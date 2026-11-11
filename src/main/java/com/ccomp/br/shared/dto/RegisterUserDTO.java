package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserDTO(
    @NotNull(message = "O nome é obrigatório.")
    @Size(min = 4, max = 255, message = "O nome deve ter entre 4 e 255 caracteres.")
    String name,

    @NotNull(message = "O e-mail é obrigatório.")
    @Valid
    EmailAddress email,

    @NotNull(message = "A senha é obrigatória.")
    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 255, message = "A senha deve ter entre 6 e 255 caracteres.")
    String password
) {
}
