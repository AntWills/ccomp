package com.ccomp.br.domain.news.persistence;

import com.ccomp.br.domain.news.dto.NewsFilter;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class NewsSpecs {
    public static Specification<News> beforeCursor(LocalDateTime cursor) {
        return (root, query, cb) -> {
            if(cursor == null)
                return cb.lessThanOrEqualTo(root.get("publishedAt"), LocalDateTime.now());
            return cb.lessThan(root.get("publishedAt"), cursor);
        };
    }

    public static Specification<News> isFeatured(Boolean featured) {
        return (root, query, criteriaBuilder) -> {
            if(featured == null) return criteriaBuilder.conjunction();

            return criteriaBuilder.equal(root.get("featured"), featured);
        };
    }

    public static Specification<News> buildSpecByCursor(NewsFilter filter, LocalDateTime cursor) {
        return Specification.where(NewsSpecs.beforeCursor(cursor))
                .and(NewsSpecs.isFeatured(filter.featured()));
    }
}
