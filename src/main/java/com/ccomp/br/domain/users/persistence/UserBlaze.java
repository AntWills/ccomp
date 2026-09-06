package com.ccomp.br.domain.users.persistence;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.users.dto.UserCursor;
import com.ccomp.br.domain.users.dto.UserItemView;
import com.ccomp.br.domain.users.dto.UserSearchFilter;
import com.ccomp.br.domain.users.persistence.roles.Roles_;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UserBlaze {
    private static final String ALIAS = "user";

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final BlazeQueryExecutor queryExecutor;

    public UserBlaze(EntityManager em, CriteriaBuilderFactory cbf, BlazeQueryExecutor queryExecutor) {
        this.em = em;
        this.cbf = cbf;
        this.queryExecutor = queryExecutor;
    }

    @Transactional(readOnly = true)
    public List<UserItemView> findByCursor(UserSearchFilter filter, @Nullable UserCursor cursor, int limit) {
        var cb = cbf.create(em, UserModel.class, ALIAS)
                .orderByDesc(path(UserModel_.createdAt))
                .orderByDesc(path(UserModel_.id))
                .setMaxResults(limit);

        filter.statusAccountOpt().ifPresent(s ->
                cb.where(path(UserModel_.statusAccount)).eq(s)
        );

        filter.roleOpt().ifPresent(r ->
                cb.where(path(UserModel_.role) + "." + Roles_.ROLE).eq(r)
        );

        if (cursor != null && cursor.createdAt() != null && cursor.id() != null) {
            cb.whereOr()
                    .where(path(UserModel_.createdAt)).lt(cursor.createdAt())
                        .whereAnd()
                            .where(path(UserModel_.createdAt)).eq(cursor.createdAt())
                            .where(path(UserModel_.id)).lt(cursor.id())
                        .endAnd()
                    .endOr();
        }

        return queryExecutor.fetchList(cb, UserItemView.class);
    }

    private String path(Attribute<?, ?> attribute) {
        return ALIAS + "." + attribute.getName();
    }
}
