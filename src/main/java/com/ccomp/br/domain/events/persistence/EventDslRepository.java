package com.ccomp.br.domain.events.persistence;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.events.dto.events.EventCursor;
import com.ccomp.br.domain.events.dto.events.EventListItemDTO;
import com.ccomp.br.domain.events.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enums.editors.EnumEditorsStatus;
import com.ccomp.br.domain.events.dto.events.EventsFilterRequest;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.ccomp.br.domain.events.persistence.QEvent.event;
import static com.ccomp.br.domain.events.persistence.editors.QEventEditor.eventEditor;
import static com.ccomp.br.domain.events.persistence.enrollments.QEnrollment.enrollment;

@Component
public class EventDslRepository {
    private static final String ALIAS = "event";

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final BlazeQueryExecutor blazeQueryExecutor;
    private final JPAQueryFactory queryFactory;


    public EventDslRepository(EntityManager em, CriteriaBuilderFactory cbf, BlazeQueryExecutor blazeQueryExecutor, JPAQueryFactory queryFactory) {
        this.em = em;
        this.cbf = cbf;
        this.blazeQueryExecutor = blazeQueryExecutor;
        this.queryFactory = queryFactory;
    }

    public List<EventListItemDTO> findByCursor(
            EventsFilterRequest filter,
            EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(event.status.eq(EnumEventStatus.PUBLISHED));

        filter.categoryOpt().ifPresent(category ->
                        whereClause.and(event.category.eq(category))
                );

        filter.formatOpt().ifPresent(format ->
                        whereClause.and(event.format.eq(format))
                );


        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            whereClause.and(
                    event.startDate.lt(cursor.startDate())
                            .or(
                                    event.startDate.eq(cursor.startDate())
                                            .and(event.id.lt(cursor.id()))
                            )
            );
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageUrl,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventListItemDTO> findAllByOwnerId(
            UUID ownerId,
            EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(event.ownerId.eq(ownerId));


        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            whereClause.and(
                    event.startDate.lt(cursor.startDate())
                            .or(
                                    event.startDate.eq(cursor.startDate())
                                            .and(event.id.lt(cursor.id()))
                            )
            );
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageUrl,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventListItemDTO> findAllSubscriptions(
            UUID participantId,
            EventCursor cursor,
            int limit
    ) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(enrollment.userId.eq(participantId));
        whereClause.and(
                enrollment.status.in(
                        EnumEnrollmentState.CONFIRMED,
                        EnumEnrollmentState.CHECKED_IN
                )
        );

        // Condição do Cursor (Keyset Pagination)
        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            whereClause.and(
                    event.startDate.lt(cursor.startDate())
                            .or(
                                    event.startDate.eq(cursor.startDate())
                                            .and(event.id.lt(cursor.id()))
                            )
            );
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageUrl,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .innerJoin(event.enrollments, enrollment)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<EventListItemDTO> findAllWhereUserIsEditor(UUID editorId, EventCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(eventEditor.userId.eq(editorId));
        whereClause.and(eventEditor.status.eq(EnumEditorsStatus.ACTIVE));

        // Condição do Cursor (Keyset Pagination)
        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            whereClause.and(
                    event.startDate.lt(cursor.startDate())
                            .or(
                                    event.startDate.eq(cursor.startDate())
                                            .and(event.id.lt(cursor.id()))
                            )
            );
        }

        return queryFactory
                .select(Projections.constructor(
                                EventListItemDTO.class,
                                event.id,
                                event.title,
                                event.slug,
                                event.summary,
                                event.content,
                                event.category,
                                event.format,
                                event.status,
                                event.coverImageUrl,
                                event.enrollmentStartDate,
                                event.enrollmentEndDate,
                                event.enrollmentPaused,
                                event.startDate,
                                event.endDate,
                                event.address,
                                event.onlineUrl
                        )
                )
                .from(event)
                .innerJoin(event.editors, eventEditor)
                .where(whereClause)
                .orderBy(
                        event.startDate.desc(),
                        event.id.desc()
                )
                .limit(limit)
                .fetch();
    }


    private String path(Attribute<?, ?> attribute) {
        return ALIAS + "." + attribute.getName();
    }

    private String path(Attribute<?, ?> relation, Attribute<?, ?> attribute) {
        return ALIAS + "." + relation.getName() + "." + attribute.getName();
    }
}
