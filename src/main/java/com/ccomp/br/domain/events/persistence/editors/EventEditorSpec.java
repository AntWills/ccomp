package com.ccomp.br.domain.events.persistence.editors;

import com.ccomp.br.domain.events.dto.editors.EventEditorCursor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class EventEditorSpec {

    public static Specification<EventEditor> hasEventId(Long eventId) {
        return (root, query, cb) -> {
            if (eventId == null) return cb.conjunction();
            return cb.equal(root.get("event").get("id"), eventId);
        };
    }

    public static Specification<EventEditor> cursorBefore(LocalDateTime cursorAssignedAt, Long cursorId) {
        return (root, query, cb) -> {
            if (cursorAssignedAt == null || cursorId == null) return cb.conjunction();
            return cb.or(
                    cb.lessThan(root.get("assignedAt"), cursorAssignedAt),
                    cb.and(
                            cb.equal(root.get("assignedAt"), cursorAssignedAt),
                            cb.lessThan(root.get("id"), cursorId)
                    )
            );
        };
    }

    public static Specification<EventEditor> buildSpec(Long eventId, EventEditorCursor cursor) {
        return Specification.where(hasEventId(eventId))
                .and(cursorBefore(
                        cursor != null ? cursor.assignedAt() : null,
                        cursor != null ? cursor.id() : null
                ));
    }
}
