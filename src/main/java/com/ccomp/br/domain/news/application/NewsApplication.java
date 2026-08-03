package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.dto.NewsFilter;
import com.ccomp.br.domain.news.dto.NewsListItem;
import com.ccomp.br.domain.news.dto.NewsPageResponse;
import com.ccomp.br.domain.news.dto.NewsUpdateDto;
import com.ccomp.br.domain.news.enums.ContentBlockType;
import com.ccomp.br.domain.news.persistence.ContentBlock;
import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.persistence.NewsSpecs;
import com.ccomp.br.domain.news.util.NewsMapper;
import com.ccomp.br.domain.news.util.SlugUtils;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorCodec;
import com.ccomp.br.shared.utils.CursorPage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NewsApplication {
    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    public NewsApplication(NewsRepository newsRepository, NewsMapper newsMapper) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
    }

    @Transactional(readOnly = true)
    public CursorPage<NewsListItem> searchNewsWithFilters(NewsFilter filter, String cursor, int pageSize) {
        if(pageSize > 50) pageSize = 50;

        Specification<News> spec = NewsSpecs.buildSpecByCursor(filter,
                CursorCodec.decode(cursor, LocalDateTime.class).orElse(null));

        int finalPageSize = pageSize;
        List<NewsListItem> results = newsRepository.findBy(spec, query -> query
                .as(NewsListItem.class)
                .limit(finalPageSize + 1)
                .sortBy(Sort.by(Sort.Direction.DESC, "publishedAt"))
                .all());

        boolean hasNext = results.size() > finalPageSize;
        List<NewsListItem> page = hasNext ? results.subList(0, finalPageSize) : results;
        String nextCursor = hasNext ? CursorCodec.encode(page.getLast().publishedAt()) : null;

        return new CursorPage<>(page, nextCursor, null);
    }

    @Transactional(readOnly = true)
    public Optional<News> getById(Long id) {
        return newsRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<News> getBySlug(String slug) {
        return newsRepository.findBySlug(slug);
    }

    @Transactional
    public News create(UUID authorId) {
        List<ContentBlock> blocks = List.of(
                new ContentBlock(1L, ContentBlockType.HEADING, "News headline", null, null, null, null),
                new ContentBlock(2L, ContentBlockType.PARAGRAPH, "text text text text text text text text text text text text text text text text text text text text text text text text text text text text text text", null, null, null, null)
        );

        News newsNoSave = News.builder()
                .title("News Title")
                .slug(generateSlug("News Title"))
                .authorId(authorId)
                .blocks(blocks)
                .build();

        return newsRepository.save(newsNoSave);
    }

    @Transactional
    public News update(Long id, NewsUpdateDto dto, UUID userId) {
        News entity = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));

        if(!userId.equals(entity.getAuthorId()))
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

        return entity;
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
    public void publish(Long id, UUID userId) {
        News model = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));

        if(!userId.equals(model.getAuthorId())) throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        model.publishNow();

        newsRepository.save(model);
    }
}
