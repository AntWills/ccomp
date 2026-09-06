package com.ccomp.br.domain.events.external;

import com.ccomp.br.domain.events.dto.events.EventCursor;
import com.ccomp.br.domain.events.persistence.EventBlaze;
import com.ccomp.br.shared.dto.EventListItemView;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class EventsManagement {
    private final int MAX_PAGE_SIZE = 50;

    private final EventBlaze eventBlaze;

    public EventsManagement(EventBlaze eventBlaze) {
        this.eventBlaze = eventBlaze;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemView> findAllByOwnerId(UUID ownerId, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class);

        // Busca N + 1 registros para verificar existência de próxima página
        List<EventListItemView> events = eventBlaze.findAllByOwnerId(ownerId, decodedCursor, finalPageSize + 1);


        return CursorUtils.buildPage(events, finalPageSize, e -> new EventCursor(e.getStartDate(), e.getId()));
    }

    @Transactional(readOnly = true)
    public CursorPage<EventListItemView> findAllSubscriptions(UUID participantId, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        EventCursor decodedCursor = CursorUtils.decode(cursor, EventCursor.class);

        List<EventListItemView> events = eventBlaze.findAllSubscriptions(participantId, decodedCursor, finalPageSize + 1);


        return CursorUtils.buildPage(events, finalPageSize, e -> new EventCursor(e.getStartDate(), e.getId()));
    }
}
