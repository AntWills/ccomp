package com.ccomp.br.domain.clubs.persistence;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ClubSpec {
    public static Specification<Club> beforeCursor(LocalDateTime cursor) {
        return (root, query, cb) -> {
            if(cursor == null)
                return cb.lessThanOrEqualTo(root.get("publishedAt"), LocalDateTime.now());
            return cb.lessThan(root.get("publishedAt"), cursor);
        };
    }

    public static Specification<Club> buildSpecByCursor(LocalDateTime cursor) {
        return Specification.where(beforeCursor(cursor));
//                .and(NewsSpecs.isFeatured(filter.featured()));
    }
}
