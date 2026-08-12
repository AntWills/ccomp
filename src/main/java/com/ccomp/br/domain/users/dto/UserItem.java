package com.ccomp.br.domain.users.dto;

import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.module.email.EmailAddress;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserItem(
        UUID id,
        String name,
        EmailAddress emailAddress,
        EnumUserStatusAccount statusAccount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        EnumRoles role
        ) {
}
