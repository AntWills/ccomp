package com.ccomp.br.domain.users.dto;

import com.ccomp.br.domain.security.roles.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;

public record UserSearchFilter(
        EnumUserStatusAccount status
//        EnumRoles role
) {
}
