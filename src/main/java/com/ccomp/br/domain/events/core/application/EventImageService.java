package com.ccomp.br.domain.events.core.application;

import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventImageService {
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp");

    private final EventAccessPolicy eventAccessPolicy;
    private final EventRepository eventRepository;
    private final StorageExternal storageExternal;
    private final StorageKeyGenerator storageKeyGenerator;

    public EventImageService(EventAccessPolicy eventAccessPolicy,
                             EventRepository eventRepository,
                             StorageExternal storageExternal,
                             StorageKeyGenerator storageKeyGenerator) {
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventRepository = eventRepository;
        this.storageExternal = storageExternal;
        this.storageKeyGenerator = storageKeyGenerator;
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

        if (previousKey != null) {
            storageExternal.deleteObject(previousKey);
        }
    }

    public Optional<StorageExternal.StoredFile> getCoverImage(Long eventId, @Nullable UUID userId) {
        Event event = findEventOrThrow(eventId);
        eventAccessPolicy.assertCanView(event, userId);

        if (event.getCoverImageKey() == null) {
            return Optional.empty();
        }

        return storageExternal.getObject(event.getCoverImageKey());
    }

    public boolean removeCoverImage(Long eventId, UUID userId) {
        Event event = findEventOrThrow(eventId);
        eventAccessPolicy.assertCanEdit(event, userId);

        if (event.getCoverImageKey() == null) {
            return false;
        }

        storageExternal.deleteObject(event.getCoverImageKey());
        event.setCoverImageKey(null);
        eventRepository.save(event);

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