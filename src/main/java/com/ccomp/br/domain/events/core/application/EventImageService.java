package com.ccomp.br.domain.events.core.application;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventCoverImageCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventImageService {
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp");

    private final EventAccessPolicy eventAccessPolicy;
    private final EventRepository eventRepository;
    private final EventCache eventCache;
    private final StorageExternal storageExternal;
    private final StorageKeyGenerator storageKeyGenerator;
    private final EventCoverImageCache imageCache;

    public EventImageService(EventAccessPolicy eventAccessPolicy,
                             EventRepository eventRepository,
                             EventCache eventCache,
                             StorageExternal storageExternal,
                             StorageKeyGenerator storageKeyGenerator,
                             EventCoverImageCache imageCache) {
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
        this.storageExternal = storageExternal;
        this.storageKeyGenerator = storageKeyGenerator;
        this.imageCache = imageCache;
    }

    public void uploadCoverImage(Long eventId, UUID userId, MultipartFile file) {
        Event event = findEventOrThrow(eventId);
        eventAccessPolicy.assertCanEdit(event, userId);

        if (file == null || file.isEmpty()) {
            throw new DomainException("Nenhum arquivo enviado.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new DomainException("Tipo de arquivo não suportado. Utilize PNG, JPEG ou WebP.");
        }

        String previousKey = event.getCoverImageKey();
        String newKey = storageKeyGenerator.generate(
                "events/%d/cover".formatted(eventId), file.getOriginalFilename());

        storageExternal.putObject(newKey, file);

        event.setCoverImageKey(newKey);
        eventRepository.save(event);
        eventCache.evict(event);

        if (previousKey != null) {
            imageCache.evict(previousKey);
            storageExternal.deleteObject(previousKey);
        }
    }

    public Optional<StorageExternal.StoredFile> getCoverImage(Long eventId, @Nullable UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!eventAccessPolicy.canView(event, userId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
        }

        if (event.coverImageKey() == null) {
            return Optional.empty();
        }

        try {
            EventCoverImageCache.CachedImage image = imageCache.get(event.coverImageKey());
            if (image == null) {
                return Optional.empty();
            }
            return Optional.of(new StorageExternal.StoredFile(
                    new ByteArrayResource(image.content()), image.contentType()));
        } catch (RuntimeException cacheException) {
            log.warn("Cache da imagem de capa indisponível; buscando direto no storage. eventId={}", eventId,
                    cacheException);
            return storageExternal.getObject(event.coverImageKey());
        }
    }

    public boolean removeCoverImage(Long eventId, UUID userId) {
        Event event = findEventOrThrow(eventId);
        eventAccessPolicy.assertCanEdit(event, userId);

        if (event.getCoverImageKey() == null) {
            return false;
        }

        String imageKey = event.getCoverImageKey();
        event.setCoverImageKey(null);
        eventRepository.save(event);
        eventCache.evict(event);
        imageCache.evict(imageKey);
        storageExternal.deleteObject(imageKey);

        return true;
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }

    private String buildCoverImageKey(Long eventId, String contentType) {
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        return "events/%d/cover/%s.%s".formatted(eventId, UUID.randomUUID(), extension);
    }

    /**
     * Par fileKey/uploadUrl — fileKey é o que o cliente devolve depois em setCoverImage,
     * uploadUrl é a URL pré-assinada de uso único (5 min) para o PUT.
     */
    public record UploadUrl(String fileKey, String uploadUrl) {}
}
