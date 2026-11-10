package com.ccomp.br.domain.events.guests.persistence;

import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.events.guests.dto.GuestImageItem;
import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class GuestImageCache {
    private final StorageExternal storageExternal;
    private final EventGuestRepository eventGuestRepository;

    public GuestImageCache(StorageExternal storageExternal, EventGuestRepository eventGuestRepository) {
        this.storageExternal = storageExternal;
        this.eventGuestRepository = eventGuestRepository;
    }

    @Cacheable(cacheNames = CacheNames.EVENT_GUEST_IMAGE, key = "#imageKey", unless = "#result == null")
    public CachedImage get(String imageKey) {
        return storageExternal.getObject(imageKey)
                .map(file -> {
                    try {
                        return new CachedImage(file.resource().getContentAsByteArray(), file.contentType());
                    } catch (IOException e) {
                        throw new IllegalStateException("Não foi possível ler a imagem do convidado.", e);
                    }
                })
                .orElse(null);
    }

    @Cacheable(cacheNames = CacheNames.EVENT_GUEST_IMAGES_BULK, key = "#eventId")
    public List<GuestImageItem> getAllForEvent(Long eventId) {
        return loadAllForEvent(eventId);
    }

    public List<GuestImageItem> loadAllForEvent(Long eventId) {
        return eventGuestRepository.findAllByEventId(eventId).stream()
                .filter(guest -> guest.getImageKey() != null)
                .map(guest -> storageExternal.getObject(guest.getImageKey())
                        .map(file -> {
                            try {
                                return new GuestImageItem(guest.getId(), file.contentType(),
                                        file.resource().getContentAsByteArray());
                            } catch (IOException e) {
                                throw new IllegalStateException("Não foi possível ler a imagem do convidado.", e);
                            }
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @CacheEvict(cacheNames = CacheNames.EVENT_GUEST_IMAGE, key = "#imageKey")
    public void evict(String imageKey) {}

    @CacheEvict(cacheNames = CacheNames.EVENT_GUEST_IMAGES_BULK, key = "#eventId")
    public void evictEvent(Long eventId) {}

    public record CachedImage(byte[] content, String contentType) {}
}
