package com.ccomp.br.domain.audit.persistence;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogResponseView;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AuditLogBlaze {
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
        var cb = cbf.create(em, AuditLog.class, "log")
                .orderByDesc("log.timestamp")
                .orderByDesc("log.id")
                .setMaxResults(limit);

        filter.optActorId().ifPresent(id -> cb.where("log.actorId").eq(id));
        filter.optTargetId().ifPresent(id -> cb.where("log.targetId").eq(id));
        filter.optAction().ifPresent(action -> cb.where("log.action").eq(action));
        filter.optStartDate().ifPresent(date -> cb.where("log.timestamp").ge(date));
        filter.optEndDate().ifPresent(date -> cb.where("log.timestamp").le(date));

        if (cursor != null) {
            cb.whereOr()
                        .where("log.timestamp").lt(cursor.timestamp())
                        .whereAnd()
                            .where("log.timestamp").eq(cursor.timestamp())
                            .where("log.id").lt(cursor.id())
                        .endAnd()
                    .endOr();
        }

        return queryExecutor.fetchList(cb, AuditLogResponseView.class);
    }
}
