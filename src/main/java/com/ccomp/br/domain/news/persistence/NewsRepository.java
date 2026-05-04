package com.ccomp.br.domain.news.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    Page<News> findAllByPublishedAtIsNotNull(Pageable pageable);
    List<News> findAllByPublishedAtLessThanEqual(LocalDateTime now, Pageable pageable);
}