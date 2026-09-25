package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.news.dto.*;
import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsDslRepository;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.utils.NewsMapper;
import com.ccomp.br.domain.news.utils.SlugUtils;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NewsApplication {
    private final NewsRepository newsRepository;
    private final NewsDslRepository newsDslRepository;
    private final NewsMapper newsMapper;

    public NewsApplication(NewsRepository newsRepository, NewsDslRepository newsDslRepository, NewsMapper newsMapper) {
        this.newsRepository = newsRepository;
        this.newsDslRepository = newsDslRepository;
        this.newsMapper = newsMapper;
    }

    @Transactional(readOnly = true)
    public CursorPage<NewsItem> searchNewsWithFilters(NewsSearchFilter filter, String cursor, int pageSize) {
        int finalPageSize = Math.min(pageSize, 50);

        NewsCursor cursorDecoded = CursorUtils.decode(cursor, NewsCursor.class);
        List<NewsItem> result = newsDslRepository.findByCursor(filter, cursorDecoded, finalPageSize + 1);

        return CursorUtils.buildPage(
                result,
                finalPageSize,
                ni -> new NewsCursor(ni.publishedAt(), ni.id())
        );
    }

    @Transactional(readOnly = true)
    public Optional<NewsResponse> getById(Long id) {
        return newsRepository.findById(id)
                .map(newsMapper::newsToNewsResponse);
    }

    @Transactional(readOnly = true)
    public Optional<NewsResponse> getBySlug(String slug) {
        return newsRepository.findBySlug(slug)
                .filter(News::hasPublished)
                .map(newsMapper::newsToNewsResponse);
    }

    @Transactional
@CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public NewsResponse create(UUID authorId) {
//        List<ContentBlock> blocks = List.of(
//                new ContentBlock(1L, ContentBlockType.HEADING, "News headline", null, null, null, null),
//                new ContentBlock(2L, ContentBlockType.PARAGRAPH, "text text text text text text text text text text text text text text text text text text text text text text text text text text text text text text", null, null, null, null)
//        );

        News newsNoSave = News.builder()
                .title("News Title")
                .slug(generateSlug("News Title"))
                .authorId(authorId)
                .blocks(List.of())
                .content("Default content")
                .build();

        News newsSaved = newsRepository.save(newsNoSave);
        return newsMapper.newsToNewsResponse(newsSaved);
    }

    @Transactional
@CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public NewsResponse update(Long id, NewsUpdateDto dto, UUID userId) {
        News entity = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));

        if(!userId.equals(entity.getAuthorId()) && !SecurityUtils.isModeratorOrAdmin())
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        if(Optional.ofNullable(dto.title()).isPresent() && !dto.title().equals(entity.getTitle())) {
            String newSlug = generateSlug(dto.title());
            entity.setSlug(newSlug);
        }

        if(dto.title() != null && !dto.title().equals(entity.getTitle())) {
            String newSlug = generateSlug(dto.title());
            entity.setSlug(newSlug);
        }

        newsMapper.updateEntityFromDto(dto, entity);

        newsRepository.save(entity);

        return newsMapper.newsToNewsResponse(entity);
    }

    @Transactional
@CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public void delete(Long newsId, UUID userId) {
        News entity = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));

        if(!entity.isAuthor(userId) && !SecurityUtils.isModeratorOrAdmin())
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        newsRepository.deleteById(newsId);
    }

    private String generateSlug(String title) {
        String base = SlugUtils.toSlug(title);

        while (true) {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            String slug = base + "-" + suffix;

            if (newsRepository.findBySlug(slug).isEmpty()) {
                return slug;
            }
        }
    }

    @Transactional
@CacheEvict(cacheNames = {"public-highlights", "public-highlight-clubs", "public-highlight-news", "public-highlight-events"}, allEntries = true)
    public void publish(Long id, UUID userId) {
        News model = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));

        if(!userId.equals(model.getAuthorId()) && !SecurityUtils.isModeratorOrAdmin()) throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        model.publishNow();

        newsRepository.save(model);
    }
}
