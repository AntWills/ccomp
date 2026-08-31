package com.ccomp.br.domain.events.persistence;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.events.dto.events.EventCursor;
import com.ccomp.br.domain.events.enums.EnumEnrollmentState;
import com.ccomp.br.shared.dto.EventListItemView;
import com.ccomp.br.domain.events.dto.events.EventsFilterRequest;
import com.ccomp.br.domain.events.enums.EnumEventStatus;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class EventBlaze {
    private static final String ALIAS = "event";

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final BlazeQueryExecutor blazeQueryExecutor;

    public EventBlaze(EntityManager em, CriteriaBuilderFactory cbf, BlazeQueryExecutor blazeQueryExecutor) {
        this.em = em;
        this.cbf = cbf;
        this.blazeQueryExecutor = blazeQueryExecutor;
    }

    public List<EventListItemView> findByCursor(EventsFilterRequest filter, EventCursor cursor, int limit) {
        var cb = cbf.create(em, Event.class, ALIAS)
                .orderByDesc(path(Event_.startDate))
                .orderByDesc(path(Event_.id))
                .setMaxResults(limit);

        // isOpen
//        var now = LocalDateTime.now();
//        cb.where(path(Event_.startDate)).lt(now);
//        cb.where(path(Event_.endDate)).gt(now);

        // Acesso ao publico
        cb.where(path(Event_.status)).eq(EnumEventStatus.PUBLISHED);

        // filtro
        filter.categoryOpt().ifPresent(c -> cb.where(path(Event_.category)).eq(c));
        filter.formatOpt().ifPresent(f -> cb.where(path(Event_.format)).eq(f));

        if(cursor != null && cursor.id() != null && cursor.startDate() != null) {
            cb.whereOr()
                    .where(path(Event_.startDate)).lt(cursor.startDate())
                    .whereAnd()
                        .where(path(Event_.startDate)).eq(cursor.startDate())
                        .where(path(Event_.id)).lt(cursor.id())
                        .endAnd()
                    .endOr();
        }

        return blazeQueryExecutor.fetchList(cb, EventListItemView.class);
    }

    public List<EventListItemView> findAllByOwnerId(UUID ownerId, EventCursor cursor, int limit) {
        var cb = cbf.create(em, Event.class, ALIAS)
                .where(path(Event_.ownerId)).eq(ownerId)
                .orderByDesc(path(Event_.startDate))
                .orderByDesc(path(Event_.id))
                .setMaxResults(limit);

        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            cb.whereOr()
                    .where(path(Event_.startDate)).lt(cursor.startDate())
                    .whereAnd()
                    .where(path(Event_.startDate)).eq(cursor.startDate())
                    .where(path(Event_.id)).lt(cursor.id())
                    .endAnd()
                    .endOr();
        }

        return blazeQueryExecutor.fetchList(cb, EventListItemView.class);
    }

    public List<EventListItemView> findAllSubscriptions(UUID participantId, EventCursor cursor, int limit) {
        var cb = cbf.create(em, Event.class, ALIAS)
                .where("enrollments.userId").eq(participantId)
                .where("enrollments.status").in(EnumEnrollmentState.CONFIRMED, EnumEnrollmentState.CHECKED_IN)
                .orderByDesc(path(Event_.startDate))
                .orderByDesc(path(Event_.id))
                .setMaxResults(limit);

        if (cursor != null && cursor.id() != null && cursor.startDate() != null) {
            cb.whereOr()
                    .where(path(Event_.startDate)).lt(cursor.startDate())
                    .whereAnd()
                    .where(path(Event_.startDate)).eq(cursor.startDate())
                    .where(path(Event_.id)).lt(cursor.id())
                    .endAnd()
                    .endOr();
        }

        return blazeQueryExecutor.fetchList(cb, EventListItemView.class);
    }

    private String path(Attribute<?, ?> attribute) {
        return ALIAS + "." + attribute.getName();
    }
}
