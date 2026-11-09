package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestCache;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
public class GuestImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp");

    private final EventAccessPolicy eventAccessPolicy;
    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;
    private final EventGuestCache eventGuestCache;
    private final StorageExternal storageExternal;
    private final StorageKeyGenerator storageKeyGenerator;

    public GuestImageService(
            EventAccessPolicy eventAccessPolicy,
            EventRepository eventRepository,
            EventGuestRepository eventGuestRepository,
            EventGuestCache eventGuestCache,
            StorageExternal storageExternal,
            StorageKeyGenerator storageKeyGenerator
    ) {
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventRepository = eventRepository;
        this.eventGuestRepository = eventGuestRepository;
        this.eventGuestCache = eventGuestCache;
        this.storageExternal = storageExternal;
        this.storageKeyGenerator = storageKeyGenerator;
    }

    public void uploadImage(Long eventId, Long guestId, UUID userId, MultipartFile file) {
        Event event = findEventOrThrow(eventId);
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

        if (previousKey != null) {
            storageExternal.deleteObject(previousKey);
        }
    }

    public Optional<StorageExternal.StoredFile> getImage(
            Long eventId,
            Long guestId,
            @Nullable UUID userId
    ) {
        Event event = findEventOrThrow(eventId);
        eventAccessPolicy.assertCanView(event, userId);

        String imageKey = eventGuestCache.findByIdAndEventId(eventId, guestId)
                .map(EventGuestDTO::imageKey)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));
        if (imageKey == null) {
            return Optional.empty();
        }

        return storageExternal.getObject(imageKey);
    }

    public boolean removeImage(Long eventId, Long guestId, UUID userId) {
        Event event = findEventOrThrow(eventId);
        EventGuest guest = findGuestOrThrow(eventId, guestId);
        assertCanEditGuest(event, guest, userId);
        if (guest.getImageKey() == null) {
            return false;
        }

        storageExternal.deleteObject(guest.getImageKey());
        guest.setImageKey(null);
        eventGuestRepository.save(guest);
        eventGuestCache.evict(guest);

        return true;
    }

    public void deleteGuest(Long eventId, Long guestId, UUID userId) {
        Event event = findEventOrThrow(eventId);
        EventGuest guest = findGuestOrThrow(eventId, guestId);
        assertCanEditGuest(event, guest, userId);

        if (guest.getImageKey() != null) {
            storageExternal.deleteObject(guest.getImageKey());
        }
        eventGuestRepository.delete(guest);
        eventGuestCache.evict(guest);
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }

    private EventGuest findGuestOrThrow(Long eventId, Long guestId) {
        return eventGuestRepository.findByIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado neste evento."));
    }

    private void assertCanEditGuest(Event event, EventGuest guest, UUID userId) {
        if (userId == null || !Objects.equals(guest.getUserId(), userId))
            eventAccessPolicy.assertCanEdit(event, userId);
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
