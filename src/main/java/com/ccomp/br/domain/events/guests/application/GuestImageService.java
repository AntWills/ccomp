package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.GuestImageItem;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestCache;
import com.ccomp.br.domain.events.guests.persistence.GuestImageCache;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class GuestImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp");

    private final EventAccessPolicy eventAccessPolicy;
    private final EventCache eventCache;
    private final EventGuestRepository eventGuestRepository;
    private final EventGuestCache eventGuestCache;
    private final GuestImageCache guestImageCache;
    private final StorageExternal storageExternal;
    private final StorageKeyGenerator storageKeyGenerator;

    public GuestImageService(
            EventAccessPolicy eventAccessPolicy,
            EventCache eventCache,
            EventGuestRepository eventGuestRepository,
            EventGuestCache eventGuestCache,
            GuestImageCache guestImageCache,
            StorageExternal storageExternal,
            StorageKeyGenerator storageKeyGenerator
    ) {
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventCache = eventCache;
        this.eventGuestRepository = eventGuestRepository;
        this.eventGuestCache = eventGuestCache;
        this.guestImageCache = guestImageCache;
        this.storageExternal = storageExternal;
        this.storageKeyGenerator = storageKeyGenerator;
    }

    public void uploadImage(Long eventId, Long guestId, UUID userId, MultipartFile file) {
        EventDTO event = findEventOrThrow(eventId);
        EventGuest guest = findGuestOrThrow(eventId, guestId);
        assertCanEditGuest(event, guest, userId);
        validateImage(file);

        String previousKey = guest.getImageKey();
        String imageKey = storageKeyGenerator.generate(
                "events/%d/guests/%d".formatted(eventId, guestId),
                file.getOriginalFilename()
        );

        storageExternal.putObject(imageKey, file);

        guest.setImageKey(imageKey);
        eventGuestRepository.save(guest);
        eventGuestCache.evict(guest);
        guestImageCache.evictEvent(eventId);

        if (previousKey != null) {
            guestImageCache.evict(previousKey);
            storageExternal.deleteObject(previousKey);
        }
    }

    public Optional<StorageExternal.StoredFile> getImage(
            Long eventId,
            Long guestId,
            @Nullable UUID userId
    ) {
        EventDTO event = findEventOrThrow(eventId);
        assertCanViewEvent(event, userId);

        String imageKey = eventGuestCache.findByIdAndEventId(eventId, guestId)
                .map(EventGuestDTO::imageKey)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));
        if (imageKey == null) {
            return Optional.empty();
        }

        GuestImageCache.CachedImage cachedImage;
        try {
            cachedImage = guestImageCache.get(imageKey);
        } catch (RuntimeException cacheException) {
            log.warn("Cache da imagem do convidado indisponível; buscando no storage. imageKey={}", imageKey,
                    cacheException);
            return storageExternal.getObject(imageKey);
        }
        if (cachedImage == null) {
            return Optional.empty();
        }
        return Optional.of(new StorageExternal.StoredFile(
                new ByteArrayResource(cachedImage.content()),
                cachedImage.contentType()));
    }

    public List<GuestImageItem> getAllImages(Long eventId, UUID userId) {
        EventDTO event = findEventOrThrow(eventId);
        assertCanEditEvent(event, userId);
        try {
            return guestImageCache.getAllForEvent(eventId);
        } catch (RuntimeException cacheException) {
            log.warn("Cache em lote das imagens dos convidados indisponível; buscando direto no storage. eventId={}",
                    eventId, cacheException);
            return guestImageCache.loadAllForEvent(eventId);
        }
    }

    public boolean removeImage(Long eventId, Long guestId, UUID userId) {
        EventDTO event = findEventOrThrow(eventId);
        EventGuest guest = findGuestOrThrow(eventId, guestId);
        assertCanEditGuest(event, guest, userId);
        if (guest.getImageKey() == null) {
            return false;
        }

        guestImageCache.evict(guest.getImageKey());
        storageExternal.deleteObject(guest.getImageKey());
        guest.setImageKey(null);
        eventGuestRepository.save(guest);
        eventGuestCache.evict(guest);
        guestImageCache.evictEvent(eventId);

        return true;
    }

    public void deleteGuest(Long eventId, Long guestId, UUID userId) {
        EventDTO event = findEventOrThrow(eventId);
        EventGuest guest = findGuestOrThrow(eventId, guestId);
        assertCanEditGuest(event, guest, userId);

        if (guest.getImageKey() != null) {
            guestImageCache.evict(guest.getImageKey());
            storageExternal.deleteObject(guest.getImageKey());
        }
        eventGuestRepository.delete(guest);
        eventGuestCache.evict(guest);
        guestImageCache.evictEvent(eventId);
    }

    private EventDTO findEventOrThrow(Long eventId) {
        return eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }

    private EventGuest findGuestOrThrow(Long eventId, Long guestId) {
        return eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));
    }

    private void assertCanEditGuest(EventDTO event, EventGuest guest, UUID userId) {
        if (userId == null || !Objects.equals(guest.getUserId(), userId)) assertCanEditEvent(event, userId);
    }

    private void assertCanViewEvent(EventDTO event, UUID userId) {
        if (!eventAccessPolicy.canView(event, userId))
            throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
    }

    private void assertCanEditEvent(EventDTO event, UUID userId) {
        if (!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("Nenhum arquivo enviado.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new DomainException("Tipo de arquivo não suportado. Utilize PNG, JPEG ou WebP.");
        }
    }
}
