package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.persistence.editor.NewsEditorsRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NewsAccessPolicy {
    private final NewsRepository newsRepository;
    private final NewsEditorsRepository newsEditorsRepository;

    public NewsAccessPolicy(NewsRepository newsRepository, NewsEditorsRepository newsEditorsRepository) {
        this.newsRepository = newsRepository;
        this.newsEditorsRepository = newsEditorsRepository;
    }

    public boolean hasAccess(UUID userId, Long newsId) {
        return newsRepository.existsByIdAndAuthorId(newsId, userId)
                || newsEditorsRepository.existsByNewsIdAndUserId(newsId, userId);
    }
}
