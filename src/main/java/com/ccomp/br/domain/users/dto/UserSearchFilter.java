package com.ccomp.br.domain.users.dto;

import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;

public record UserSearchFilter(
        EnumUserStatusAccount statusAccount
//        EnumRoles role
) {
}
