package com.ccomp.br.shared.dto;

import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.module.email.EmailAddress;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        EmailAddress emailAddress,
        EnumUserStatusAccount statusAccount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        EnumRoles role,
        @JsonIgnore
        String password
) {
    public boolean isTeamMember() {
        return this.role == EnumRoles.STAFF
                || this.role == EnumRoles.ADMIN;
    }

    public boolean isActive() {
        return this.statusAccount == EnumUserStatusAccount.ACTIVE;
    }
}
