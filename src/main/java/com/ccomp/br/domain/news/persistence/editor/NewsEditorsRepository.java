package com.ccomp.br.domain.news.persistence.editor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NewsEditorsRepository extends JpaRepository<NewsEditors, Long> {
    List<NewsEditors> findAllByNewsId(Long newsId);

    boolean existsByNewsIdAndUserId(Long newsId, UUID userId);

    long deleteByUserIdAndNewsId(UUID userId, Long newsId);
}