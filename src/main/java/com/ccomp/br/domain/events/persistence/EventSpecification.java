package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class EventSpecification {
    public static Specification<Event> isOpen() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), LocalDateTime.now()),
                criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), LocalDateTime.now())
        );
    }

    public static Specification<Event> hasCategory(EnumEventCategory category) {
        return (root, query, criteriaBuilder) -> category == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<Event> afterDateTime(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.greaterThan(root.get("startDate"), dateTime);
        };
    }

    public static Specification<Event> cursorBefore(LocalDateTime cursorStartDate, Long cursorId) {
        return (root, query, cb) -> {
            if (cursorStartDate == null || cursorId == null) return cb.conjunction();
            return cb.or(
                    cb.lessThan(root.get("startDate"), cursorStartDate),
                    cb.and(
                            cb.equal(root.get("startDate"), cursorStartDate),
                            cb.lessThan(root.get("id"), cursorId)
                    )
            );
        };
    }
}
