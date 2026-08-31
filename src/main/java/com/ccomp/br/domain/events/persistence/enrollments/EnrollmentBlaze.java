package com.ccomp.br.domain.events.persistence.enrollments;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.ccomp.br.domain.audit.persistence.AuditLog;
import com.ccomp.br.domain.audit.persistence.AuditLog_;
import com.ccomp.br.domain.events.dto.enrollments.EnrollmentsCursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnrollmentBlaze {
    private static final String ALIAS = "event_enrollment";

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;

    public EnrollmentBlaze(EntityManager em, CriteriaBuilderFactory cbf) {
        this.em = em;
        this.cbf = cbf;
    }

    public List<Enrollment> findByCursor(Long eventId, EnrollmentsCursor cursor,  int limit) {
        var cb = cbf.create(em, Enrollment.class, ALIAS)
                .orderByDesc(path(Enrollment_.createdAt))
                .orderByDesc(path(Enrollment_.id))
                .setMaxResults(limit);

        cb.where(path(Enrollment_.event) + ".id").eq(eventId);

        if (cursor != null && cursor.createdAt() != null && cursor.id() != null) {
            cb.whereOr()
                    .where(path(Enrollment_.createdAt)).lt(cursor.createdAt())
                        .whereAnd()
                            .where(path(Enrollment_.createdAt)).eq(cursor.createdAt())
                            .where(path(Enrollment_.id)).lt(cursor.id())
                        .endAnd()
                    .endOr();
        }

        return cb.getResultList();
    }

    private String path(Attribute<?, ?> attribute) {
        return ALIAS + "." + attribute.getName();
    }
}
