package com.ccomp.br.domain.audit.persistence;

import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLogSpec {

    public static Specification<AuditLog> isActor(UUID actorId) {
        return (root, query, cb) -> {
            if (actorId == null) return cb.conjunction();
            return cb.equal(root.get("actorId"), actorId);
        };
    }

    public static Specification<AuditLog> isTarget(UUID targetId) {
        return (root, query, cb) -> {
            if (targetId == null) return cb.conjunction();
            return cb.equal(root.get("targetId"), targetId);
        };
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> {
            if (action == null || action.isBlank()) return cb.conjunction();
            return cb.equal(root.get("action"), action);
        };
    }

    public static Specification<AuditLog> timestampAfterOrEqual(LocalDateTime startDate) {
        return (root, query, cb) -> {
            if (startDate == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("timestamp"), startDate);
        };
    }

    public static Specification<AuditLog> timestampBeforeOrEqual(LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (endDate == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("timestamp"), endDate);
        };
    }

    public static Specification<AuditLog> cursorBefore(AuditLogCursor cursor) {
        return (root, query, cb) -> {
            if (cursor == null || cursor.timestamp() == null || cursor.id() == null) {
                return cb.conjunction();
            }

            return cb.or(
                    cb.lessThan(root.get("timestamp"), cursor.timestamp()),
                    cb.and(
                            cb.equal(root.get("timestamp"), cursor.timestamp()),
                            cb.lessThan(root.get("id"), cursor.id())
                    )
            );
        };
    }

    public static Specification<AuditLog> buildSpec(AuditLogSearchFilter filter, AuditLogCursor cursor) {
        return Specification.where(isActor(filter.actorId()))
                .and(isTarget(filter.targetId()))
                .and(hasAction(filter.action()))
                .and(timestampAfterOrEqual(filter.startDate()))
                .and(timestampBeforeOrEqual(filter.endDate()))
                .and(cursorBefore(cursor));
    }
}