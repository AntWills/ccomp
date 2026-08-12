package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import jakarta.persistence.criteria.JoinType;
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

    public static Specification<UserModel> hasAnyRole(List<EnumRoles> roles) {
        return (root, query, cb) -> {
            if (roles == null || roles.isEmpty())
                return cb.conjunction();

            var roleJoin = root.join("role", JoinType.LEFT);
            return roleJoin.get("role").in(roles);
        };
    }

    public static Specification<UserModel> fetchRole() {
        return (root, query, cb) -> {
            // evita aplicar fetch em queries de contagem (se algum dia usar Page/count)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("role", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    public static Specification<UserModel> buildSpecByCursor(UserSearchFilter filter, LocalDateTime cursor) {
        return Specification.where(beforeCursor(cursor))
                .and(hasStausAccount(filter.statusAccount()));
    }
}
