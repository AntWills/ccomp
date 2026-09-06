package com.ccomp.br.domain.users.dto;

import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import lombok.Builder;

import java.util.Optional;

@Builder
public record UserSearchFilter(
        EnumUserStatusAccount statusAccount,
        EnumRoles role
) {
    public Optional<EnumRoles> roleOpt() {
        return Optional.ofNullable(role);
    }

    public Optional<EnumUserStatusAccount> statusAccountOpt() {
        return Optional.ofNullable(statusAccount);
    }
}
