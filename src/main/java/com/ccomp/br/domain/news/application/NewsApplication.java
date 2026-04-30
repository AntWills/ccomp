package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.dto.NewsUpdateDto;
import com.ccomp.br.domain.news.enums.ContentBlockType;
import com.ccomp.br.domain.news.persistence.ContentBlock;
import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.util.NewsMapper;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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

    @Transactional
    public News create(UUID authorId) {
        List<ContentBlock> blocks = List.of(
                new ContentBlock(1L, ContentBlockType.HEADING.toString(), "News headline", null, null, null, null),
                new ContentBlock(2L, ContentBlockType.PARAGRAPH.toString(), "text text text text text text text text text text text text text text text text text text text text text text text text text text text text text text", null, null, null, null)
        );

        News newsNoSave = News.builder()
                .title("News Title")
                .authorId(authorId)
                .blocks(blocks)
                .build();

        return newsRepository.save(newsNoSave);
    }

    @Transactional
    public News update(NewsUpdateDto dto, UUID userId) {
        News entity = newsRepository.findById(dto.id())
                .orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));

        if(!userId.equals(entity.getAuthorId())) throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        newsMapper.updateEntityFromDto(dto, entity);

        newsRepository.save(entity);

        return entity;
    }

    public Optional<News> getById(Long id) {
        return newsRepository.findById(id);
    }
}
