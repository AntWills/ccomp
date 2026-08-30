package com.ccomp.br.domain.audit.persistence;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogResponseView;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AuditLogBlaze {
    private static final String ALIAS = "log";

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final BlazeQueryExecutor queryExecutor;

    public AuditLogBlaze(EntityManager em, CriteriaBuilderFactory cbf, BlazeQueryExecutor queryExecutor) {
        this.em = em;
        this.cbf = cbf;
        this.queryExecutor = queryExecutor;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseView> findByCursor(AuditLogSearchFilter filter, AuditLogCursor cursor, int limit) {
        var cb = cbf.create(em, AuditLog.class, ALIAS)
                .orderByDesc(path(AuditLog_.timestamp))
                .orderByDesc(path(AuditLog_.id))
                .setMaxResults(limit);

        filter.optActorId().ifPresent(id -> cb.where(path(AuditLog_.actorId)).eq(id));
        filter.optTargetId().ifPresent(id -> cb.where(path(AuditLog_.targetId)).eq(id));
        filter.optAction().ifPresent(action -> cb.where(path(AuditLog_.action)).eq(action));
        filter.optStartDate().ifPresent(date -> cb.where(path(AuditLog_.timestamp)).ge(date));
        filter.optEndDate().ifPresent(date -> cb.where(path(AuditLog_.timestamp)).le(date));

        if (cursor != null && cursor.timestamp() != null && cursor.id() != null) {
            cb.whereOr()
                    .where(path(AuditLog_.timestamp)).lt(cursor.timestamp())
                        .whereAnd()
                            .where(path(AuditLog_.timestamp)).eq(cursor.timestamp())
                            .where(path(AuditLog_.id)).lt(cursor.id())
                        .endAnd()
                    .endOr();
        }

        return queryExecutor.fetchList(cb, AuditLogResponseView.class);
    }

    private String path(Attribute<?, ?> attribute) {
        return ALIAS + "." + attribute.getName();
    }
}
