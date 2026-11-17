package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.persistence.editor.NewsEditorsRepository;
import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.jspecify.annotations.Nullable;
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
        return SecurityUtils.isModeratorOrAdmin()
                || newsRepository.existsByIdAndAuthorId(newsId, userId)
                || newsEditorsRepository.existsByNewsIdAndUserId(newsId, userId);
    }

    public boolean canView(News news, @Nullable UUID userId) {
        return news.hasPublished() || (userId != null && hasAccess(userId, news.getId()));
    }

    public void assertCanEdit(News news, @Nullable UUID userId) {
        if (userId != null && hasAccess(userId, news.getId())) return;
        throw denied();
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
    }
}
