package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UserSpec {
    public static Specification<UserModel> hasStausAccount(EnumUserStatusAccount status) {
        return (root, query, cb) -> {
            if(status == null)
                return cb.conjunction();
            return cb.equal(root.get("statusAccount"), status);
        };
    }

    public static Specification<UserModel> ids(List<UUID> userIds) {
        return (root, query, cb) -> {
            if (userIds == null || userIds.isEmpty())
                return cb.conjunction();
            return root.get("id").in(userIds);
        };
    }

    public static Specification<UserModel> beforeCursor(LocalDateTime cursor) {
        return (root, query, cb) -> {
            if(cursor == null)
                return cb.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.now());
            return cb.lessThan(root.get("createdAt"), cursor);
        };
    }

    public static Specification<UserModel> buildSpecByCursor(UserSearchFilter filter, LocalDateTime cursor) {
        return Specification.where(beforeCursor(cursor))
                .and(hasStausAccount(filter.status()));
    }
}
