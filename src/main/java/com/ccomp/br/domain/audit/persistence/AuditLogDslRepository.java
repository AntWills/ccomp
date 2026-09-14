package com.ccomp.br.domain.audit.persistence;

import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.audit.persistence.QAuditLog.auditLog;

@Repository
public class AuditLogDslRepository {
    private final JPAQueryFactory queryFactory;

    public AuditLogDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<AuditLog> findAllWithCursor(AuditLogSearchFilter filter, @Nullable AuditLogCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        filter.optActorId().ifPresent(id -> whereClause.and(auditLog.actorId.eq(id)));
        filter.optTargetId().ifPresent(targ -> whereClause.and(auditLog.targetId.eq(targ.toString())));
        filter.optAction().ifPresent(action -> whereClause.and(auditLog.action.eq(action)));
        filter.optStartDate().ifPresent(date -> whereClause.and(auditLog.timestamp.goe(date)));
        filter.optEndDate().ifPresent(date -> whereClause.and(auditLog.timestamp.loe(date)));

        if(cursor != null && cursor.timestamp() != null && cursor.id() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    auditLog.timestamp,
                    auditLog.id,
                    cursor.timestamp(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.
                selectFrom(auditLog)
                .where(whereClause)
                .orderBy(
                        auditLog.timestamp.desc(),
                        auditLog.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}
