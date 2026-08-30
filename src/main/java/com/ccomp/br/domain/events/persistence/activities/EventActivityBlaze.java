package com.ccomp.br.domain.events.persistence.activities;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.events.dto.EventActivityCursor;
import com.ccomp.br.domain.events.dto.EventActivityView;
import com.ccomp.br.shared.utils.BlazeQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventActivityBlaze {
    private static final String ALIAS = "event_activity";

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final BlazeQueryExecutor blazeQueryExecutor;

    public EventActivityBlaze(EntityManager em, CriteriaBuilderFactory cbf, BlazeQueryExecutor blazeQueryExecutor) {
        this.em = em;
        this.cbf = cbf;
        this.blazeQueryExecutor = blazeQueryExecutor;
    }

    public List<EventActivityView> findByCursor(Long eventId, EventActivityCursor cursor, int limit) {
        var cb = cbf.create(em, EventActivity.class, ALIAS)
                .orderByDesc(path(EventActivity_.createdAt))
                .orderByDesc(path(EventActivity_.id))
                .setMaxResults(limit);

        cb.where(path(EventActivity_.event) + ".id").eq(eventId);

        if (cursor != null && cursor.createdAd() != null && cursor.id() != null) {
            cb.whereOr()
                    .where(path(EventActivity_.createdAt)).lt(cursor.createdAd())
                    .whereAnd()
                        .where(path(EventActivity_.createdAt)).eq(cursor.createdAd())
                        .where(path(EventActivity_.id)).lt(cursor.id())
                    .endAnd()
                    .endOr();
        }

        return blazeQueryExecutor.fetchList(cb, EventActivityView.class);
    }

    private String path(Attribute<?, ?> attribute) {
        return ALIAS + "." + attribute.getName();
    }
}
