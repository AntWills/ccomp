package com.ccomp.br.domain.users.persistence;

import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UserSpec {

    public static Specification<UserModel> hasStatusAccount(EnumUserStatusAccount status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
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
            // cursor nulo == primeira página, sem filtro de data
            if (cursor == null) return cb.conjunction();
            return cb.lessThan(root.get("createdAt"), cursor);
        };
    }

    /**
     * Faz fetch da role (para evitar N+1 na listagem) e, se houver filtro,
     * reaproveita a MESMA junção no WHERE em vez de criar um join redundante.
     * <p>
     * Sem filtro de roles -> LEFT JOIN FETCH (traz o usuário mesmo sem role).
     * Com filtro de roles  -> INNER JOIN FETCH (só usuários com role no filtro).
     * <p>
     * O cast Fetch -> Join não é garantido pela spec JPA, mas é satisfeito
     * pela implementação do Hibernate (mesma instância implementa as duas
     * interfaces). Válido enquanto o provider JPA for Hibernate.
     */
    @SuppressWarnings("unchecked")
    public static Specification<UserModel> fetchAndFilterRole(List<EnumRoles> roles) {
        return (root, query, cb) -> {
            boolean isCountQuery = query.getResultType() == Long.class || query.getResultType() == long.class;
            boolean hasRolesFilter = roles != null && !roles.isEmpty();

            // Queries de contagem não devem levar FETCH (Hibernate ignora/avisa,
            // mas evitamos por clareza); só o join é necessário quando há filtro.
            if (isCountQuery) {
                if (!hasRolesFilter) return cb.conjunction();
                return root.join("role", JoinType.INNER).get("role").in(roles);
            }

            JoinType joinType = hasRolesFilter ? JoinType.INNER : JoinType.LEFT;
            Fetch<UserModel, ?> fetchRole = root.fetch("role", joinType);

            if (!hasRolesFilter) return cb.conjunction();

            Join<UserModel, ?> roleJoin = (Join<UserModel, ?>) fetchRole;
            return roleJoin.get("role").in(roles);
        };
    }

    public static Specification<UserModel> buildSpecByCursor(UserSearchFilter filter, LocalDateTime cursor) {
        List<EnumRoles> roles = filter.role() != null
                ? List.of(filter.role())
                : Collections.emptyList();
        return Specification.where(beforeCursor(cursor))
                .and(hasStatusAccount(filter.statusAccount()))
                .and(fetchAndFilterRole(roles));
    }
}