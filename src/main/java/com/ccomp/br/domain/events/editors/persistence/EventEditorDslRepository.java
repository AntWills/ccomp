package com.ccomp.br.domain.events.editors.persistence;

import com.ccomp.br.domain.events.editors.dto.EventEditorCursor;
import com.ccomp.br.domain.events.editors.dto.EventEditorListItem;
import com.ccomp.br.shared.dto.UserSummaryDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.events.editors.persistence.QEventEditor.eventEditor;
import static com.ccomp.br.domain.users.persistence.QUserModel.userModel;

@Repository
public class EventEditorDslRepository {
    private final JPAQueryFactory queryFactory;

    public EventEditorDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<EventEditorListItem> findAllWithCursor(Long eventId, @Nullable EventEditorCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventEditor.event.id.eq(eventId));

        if(cursor != null && cursor.id() != null && cursor.assignedAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    eventEditor.assignedAt,
                    eventEditor.id,
                    cursor.assignedAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.select(
                    Projections.constructor(
                            EventEditorListItem.class,
                            Projections.constructor(
                                    UserSummaryDTO.class,
                                    userModel.id,
                                    userModel.name,
                                    userModel.emailAddress
                            ),
                            eventEditor.id,
                            eventEditor.event.id,
                            eventEditor.assignedAt,
                            eventEditor.revokedAt,
                            eventEditor.status
                    )
                )
                .from(eventEditor)
                .innerJoin(userModel).on(userModel.id.eq(eventEditor.userId))
                .where(whereClause)
                .orderBy(
                        eventEditor.assignedAt.desc(),
                        eventEditor.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}
