package com.ccomp.br.domain.news.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, Long>, JpaSpecificationExecutor<News> {
//    <T> List<T> findBy(Specification<News> spec,
//                       Function<FluentQuery.FetchableFluentQuery<News>, List<T>> query);

    boolean existsById(Long id);
    boolean existsByIdAndAuthorId(Long id, UUID authorId);
    List<News> findAllByAuthorId(UUID authorId);
    long countBySlugStartsWith(String slug);

    Optional<News> findBySlug(String slug);

    Page<News> findAllByPublishedAtIsNotNull(Pageable pageable);
    List<News> findAllByPublishedAtLessThanEqual(LocalDateTime now, Pageable pageable);
    List<News> findAllByPublishedAtLessThanEqualAndFeaturedTrue(LocalDateTime now, Pageable pageable);
}