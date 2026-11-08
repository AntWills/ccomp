package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplateRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificateCoverService {
    private static final long MAX_IMAGE_PIXELS = 30_000_000L;

    private final EventCoverTemplateRepository coverTemplateRepository;
    private final CertificateFetcher certificateFetcher;
    private final EventCache eventCache;
    private final EventAccessPolicy accessPolicy;
    private final StorageKeyGenerator storageKeyGenerator;
    private final StorageExternal storageExternal;

    public CertificateCoverService(EventCoverTemplateRepository coverTemplateRepository, CertificateFetcher certificateFetcher, EventCache eventCache, EventAccessPolicy accessPolicy, StorageKeyGenerator storageKeyGenerator, StorageExternal storageExternal) {
        this.coverTemplateRepository = coverTemplateRepository;
        this.certificateFetcher = certificateFetcher;
        this.eventCache = eventCache;
        this.accessPolicy = accessPolicy;
        this.storageKeyGenerator = storageKeyGenerator;
        this.storageExternal = storageExternal;
    }

    @Transactional
    public StorageExternal.StoredFile loadCoverTemplate(long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canView(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        return certificateFetcher.fetchCoverTemplate(eventId);
    }

    @Transactional
    public void uploadCoverTemplate(long eventId, UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("Nenhum arquivo enviado.");
        }

        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        ValidatedImage image = validateImage(file);

        String newKey = storageKeyGenerator.generate(
                "events/%d/certificate-cover-template".formatted(eventId),
                file.getOriginalFilename()
        );
        Optional<EventCoverTemplate> coverTemplateOpt = coverTemplateRepository.findByEventId(eventId);
        String oldKey = coverTemplateOpt.map(EventCoverTemplate::getCoverTemplateKey).orElse(null);

        storageExternal.putObject(newKey, image.bytes(), image.contentType());
        registerStorageCompensation(newKey, oldKey);

        EventCoverTemplate coverTemplate = coverTemplateOpt.orElseGet(() ->
                EventCoverTemplate.builder().eventId(eventId).build());
        coverTemplate.setCoverTemplateKey(newKey);
        coverTemplateRepository.save(coverTemplate);
        if (!TransactionSynchronizationManager.isSynchronizationActive() && oldKey != null) {
            storageExternal.deleteObject(oldKey);
        }
    }

    private ValidatedImage validateImage(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
            String detectedType;
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (input == null) throw unsupportedImage();
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw unsupportedImage();
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    detectedType = switch (format) {
                        case "png" -> "image/png";
                        case "jpeg", "jpg" -> "image/jpeg";
                        case "webp" -> "image/webp";
                        default -> throw unsupportedImage();
                    };
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS
                            || reader.read(0) == null) {
                        throw new DomainException("A imagem é inválida ou excede o limite de resolução.");
                    }
                } finally {
                    reader.dispose();
                }
            }
            return new ValidatedImage(bytes, detectedType);
        } catch (IOException e) {
            throw new DomainException("Não foi possível ler a imagem enviada.");
        }
    }

    private DomainException unsupportedImage() {
        return new DomainException("Tipo de arquivo não suportado. Utilize PNG, JPEG ou Webp.");
    }

    private void registerStorageCompensation(String newKey, String oldKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldKey != null) storageExternal.deleteObject(oldKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storageExternal.deleteObject(newKey);
            }
        });
    }

    private record ValidatedImage(byte[] bytes, String contentType) {}

    /**
     * Remove o fundo salvo do certificado do evento. Devolve {@code false} quando
     * o evento nunca teve um fundo próprio (ou seja, já usa a imagem padrão).
     */
    @Transactional
    public boolean deleteCoverTemplate(long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!accessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        Optional<EventCoverTemplate> coverTemplateOpt = coverTemplateRepository.findByEventId(eventId);

        if (coverTemplateOpt.isEmpty())
            return false;

        EventCoverTemplate coverTemplate = coverTemplateOpt.get();
        String storageKey = coverTemplate.getCoverTemplateKey();

        coverTemplateRepository.delete(coverTemplate);
        storageExternal.deleteObject(storageKey);

        return true;
    }
}
