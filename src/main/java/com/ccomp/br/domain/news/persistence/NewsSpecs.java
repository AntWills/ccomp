package com.ccomp.br.domain.news.persistence;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class NewsSpecs {
    public static Specification<News> beforeCursor(LocalDateTime cursor) {
        return (root, query, cb) -> cursor == null
                ? cb.lessThanOrEqualTo(root.get("publishedAt"), LocalDateTime.now())
                : cb.lessThan(root.get("publishedAt"), cursor);
    }

    public static Specification<News> isFeatured(Boolean featured) {
        return (root, query, criteriaBuilder) -> featured == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("featured"), featured);
    }
}
