package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsCoverImageCache;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NewsImageService {
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");

    private final NewsAccessPolicy accessPolicy;
    private final NewsRepository newsRepository;
    private final StorageExternal storageExternal;
    private final StorageKeyGenerator keyGenerator;
    private final NewsCoverImageCache imageCache;

    public NewsImageService(NewsAccessPolicy accessPolicy, NewsRepository newsRepository,
                            StorageExternal storageExternal, StorageKeyGenerator keyGenerator,
                            NewsCoverImageCache imageCache) {
        this.accessPolicy = accessPolicy;
        this.newsRepository = newsRepository;
        this.storageExternal = storageExternal;
        this.keyGenerator = keyGenerator;
        this.imageCache = imageCache;
    }

    public void uploadCoverImage(Long newsId, @Nullable UUID userId, MultipartFile file) {
        News news = findNewsOrThrow(newsId);
        accessPolicy.assertCanEdit(news, userId);
        if (file == null || file.isEmpty()) throw new DomainException("Nenhum arquivo enviado.");
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new DomainException("Tipo de arquivo não suportado. Utilize PNG, JPEG ou WebP.");
        }

        String previousKey = news.getCoverImageKey();
        String newKey = keyGenerator.generate("news/%d/cover".formatted(newsId), file.getOriginalFilename());
        storageExternal.putObject(newKey, file);
        news.setCoverImageKey(newKey);
        newsRepository.save(news);
        if (previousKey != null) {
            imageCache.evict(previousKey);
            storageExternal.deleteObject(previousKey);
        }
    }

    public Optional<StorageExternal.StoredFile> getCoverImage(Long newsId, @Nullable UUID userId) {
        News news = findNewsOrThrow(newsId);
        if (!accessPolicy.canView(news, userId)) throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
        if (news.getCoverImageKey() == null) return Optional.empty();
        try {
            NewsCoverImageCache.CachedImage image = imageCache.get(news.getCoverImageKey());
            if (image == null) return Optional.empty();
            return Optional.of(new StorageExternal.StoredFile(new ByteArrayResource(image.content()), image.contentType()));
        } catch (RuntimeException cacheException) {
            log.warn("Cache da imagem de capa indisponível; buscando direto no storage. newsId={}", newsId, cacheException);
            return storageExternal.getObject(news.getCoverImageKey());
        }
    }

    public boolean removeCoverImage(Long newsId, @Nullable UUID userId) {
        News news = findNewsOrThrow(newsId);
        accessPolicy.assertCanEdit(news, userId);
        if (news.getCoverImageKey() == null) return false;
        String imageKey = news.getCoverImageKey();
        news.setCoverImageKey(null);
        newsRepository.save(news);
        imageCache.evict(imageKey);
        storageExternal.deleteObject(imageKey);
        return true;
    }

    private News findNewsOrThrow(Long newsId) {
        return newsRepository.findById(newsId).orElseThrow(() -> new ResourceNotFoundException("Notícia não encontrada."));
    }
}
