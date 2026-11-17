package com.ccomp.br.domain.news.persistence;

import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NewsCoverImageCache {
    private final StorageExternal storageExternal;

    public NewsCoverImageCache(StorageExternal storageExternal) {
        this.storageExternal = storageExternal;
    }

    @Cacheable(cacheNames = CacheNames.NEWS_COVER_IMAGE, key = "#imageKey", unless = "#result == null")
    public CachedImage get(String imageKey) {
        return storageExternal.getObject(imageKey).map(file -> {
            try {
                return new CachedImage(file.resource().getContentAsByteArray(), file.contentType());
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível ler a imagem de capa da notícia.", exception);
            }
        }).orElse(null);
    }

    @CacheEvict(cacheNames = CacheNames.NEWS_COVER_IMAGE, key = "#imageKey")
    public void evict(String imageKey) {}

    public record CachedImage(byte[] content, String contentType) {}
}
