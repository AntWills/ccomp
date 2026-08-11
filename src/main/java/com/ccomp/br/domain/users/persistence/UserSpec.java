package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import org.springframework.data.jpa.domain.Specification;

public class UserSpec {
    public static Specification<UserModel> hasStausAccount(EnumUserStatusAccount status) {
        return (root, query, cb) -> {
            if(status == null)
                return cb.conjunction();
            return cb.equal(root.get("statusAccount"), status);
        };
    }
}
