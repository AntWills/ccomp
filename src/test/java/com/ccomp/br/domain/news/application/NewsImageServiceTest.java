package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsCoverImageCache;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsImageServiceTest {
    private static final Long NEWS_ID = 18L;
    private static final UUID USER_ID = UUID.randomUUID();
    @Mock private NewsAccessPolicy accessPolicy;
    @Mock private NewsRepository newsRepository;
    @Mock private StorageExternal storageExternal;
    @Mock private StorageKeyGenerator keyGenerator;
    @Mock private NewsCoverImageCache imageCache;
    private NewsImageService service;
    private News news;

    @BeforeEach
    void setUp() {
        service = new NewsImageService(accessPolicy, newsRepository, storageExternal, keyGenerator, imageCache);
        news = News.builder().id(NEWS_ID).title("News").slug("news").authorId(USER_ID).featured(false).build();
        lenient().when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.of(news));
    }

    @Test
    void uploadStoresImageAndRemovesPreviousAfterSavingNewKey() {
        news.setCoverImageKey("news/18/cover/old.png");
        var file = new MockMultipartFile("file", "cover.PNG", "image/png", new byte[]{1, 2, 3});
        when(keyGenerator.generate("news/18/cover", "cover.PNG")).thenReturn("news/18/cover/new.png");
        service.uploadCoverImage(NEWS_ID, USER_ID, file);
        assertThat(news.getCoverImageKey()).isEqualTo("news/18/cover/new.png");
        InOrder order = inOrder(accessPolicy, storageExternal, newsRepository, imageCache);
        order.verify(accessPolicy).assertCanEdit(news, USER_ID);
        order.verify(storageExternal).putObject("news/18/cover/new.png", file);
        order.verify(newsRepository).save(news);
        order.verify(imageCache).evict("news/18/cover/old.png");
        order.verify(storageExternal).deleteObject("news/18/cover/old.png");
    }

    @Test
    void uploadRejectsMissingAndEmptyFile() {
        assertThatThrownBy(() -> service.uploadCoverImage(NEWS_ID, USER_ID, null))
                .isInstanceOf(DomainException.class).hasMessage("Nenhum arquivo enviado.");
        assertThatThrownBy(() -> service.uploadCoverImage(NEWS_ID, USER_ID,
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])))
                .isInstanceOf(DomainException.class).hasMessage("Nenhum arquivo enviado.");
        verifyNoInteractions(storageExternal, keyGenerator);
        verify(newsRepository, never()).save(any());
    }

    @Test
    void uploadRejectsUnsupportedTypeAndAllowsJpegAndWebp() {
        assertThatThrownBy(() -> service.uploadCoverImage(NEWS_ID, USER_ID,
                new MockMultipartFile("file", "cover.gif", "image/gif", new byte[]{1})))
                .isInstanceOf(DomainException.class)
                .hasMessage("Tipo de arquivo não suportado. Utilize PNG, JPEG ou WebP.");
        verifyNoInteractions(storageExternal, keyGenerator);
        for (String type : new String[]{"image/jpeg", "image/webp"}) {
            var file = new MockMultipartFile("file", "cover", type, new byte[]{1});
            when(keyGenerator.generate("news/18/cover", "cover")).thenReturn("news/18/cover/" + type);
            service.uploadCoverImage(NEWS_ID, USER_ID, file);
            verify(storageExternal).putObject("news/18/cover/" + type, file);
        }
        verify(newsRepository, times(2)).save(news);
    }

    @Test
    void getImageReturnsEmptyWhenNoKeyAndCachedBytesWhenPresent() throws Exception {
        when(accessPolicy.canView(news, null)).thenReturn(true);
        when(accessPolicy.canView(news, USER_ID)).thenReturn(true);
        assertThat(service.getCoverImage(NEWS_ID, null)).isEmpty();
        verifyNoInteractions(imageCache, storageExternal);
        news.setCoverImageKey("news/18/cover/cover.webp");
        when(imageCache.get(news.getCoverImageKey())).thenReturn(new NewsCoverImageCache.CachedImage(new byte[]{4, 5}, "image/webp"));
        var result = service.getCoverImage(NEWS_ID, USER_ID).orElseThrow();
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.resource().getContentAsByteArray()).containsExactly(4, 5);
    }

    @Test
    void getImageFallsBackToStorageWhenCacheFails() {
        when(accessPolicy.canView(news, USER_ID)).thenReturn(true);
        news.setCoverImageKey("news/18/cover/cover.jpg");
        when(imageCache.get(news.getCoverImageKey())).thenThrow(new IllegalStateException("cache unavailable"));
        var stored = new StorageExternal.StoredFile(new org.springframework.core.io.ByteArrayResource(new byte[]{8}), "image/jpeg");
        when(storageExternal.getObject(news.getCoverImageKey())).thenReturn(Optional.of(stored));
        assertThat(service.getCoverImage(NEWS_ID, USER_ID)).contains(stored);
    }

    @Test
    void removeImageClearsKeyAndDeletesObjectOrReturnsFalseWhenMissing() {
        assertThat(service.removeCoverImage(NEWS_ID, USER_ID)).isFalse();
        verifyNoInteractions(storageExternal);
        news.setCoverImageKey("news/18/cover/current.jpg");
        assertThat(service.removeCoverImage(NEWS_ID, USER_ID)).isTrue();
        assertThat(news.getCoverImageKey()).isNull();
        verify(newsRepository).save(news);
        verify(imageCache).evict("news/18/cover/current.jpg");
        verify(storageExternal).deleteObject("news/18/cover/current.jpg");
    }

    @Test
    void operationsFailForMissingNews() {
        when(newsRepository.findById(NEWS_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.uploadCoverImage(NEWS_ID, USER_ID,
                new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1})))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Notícia não encontrada.");
        assertThatThrownBy(() -> service.getCoverImage(NEWS_ID, USER_ID)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.removeCoverImage(NEWS_ID, USER_ID)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(accessPolicy, storageExternal, keyGenerator, imageCache);
    }
}
