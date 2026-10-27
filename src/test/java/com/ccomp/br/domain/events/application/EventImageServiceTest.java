package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.application.EventImageService;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
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
class EventImageServiceTest {
    private static final Long EVENT_ID = 27L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private EventAccessPolicy accessPolicy;
    @Mock private EventRepository eventRepository;
    @Mock private StorageExternal storageExternal;
    @Mock private StorageKeyGenerator keyGenerator;

    private EventImageService service;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new EventImageService(accessPolicy, eventRepository, storageExternal, keyGenerator);
        event = Event.builder()
                .id(EVENT_ID)
                .title("Evento")
                .slug("evento")
                .category(EnumEventCategory.ACADEMIC_EDUCATIONAL)
                .format(EnumEventFormat.IN_PERSON)
                .status(EnumEventStatus.DRAFT)
                .ownerId(USER_ID)
                .enrollmentPaused(false)
                .build();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    }

    @Test
    void uploadSavesNewImageBeforeDeletingPreviousImage() {
        event.setCoverImageKey("events/27/cover/old.jpg");
        var file = new MockMultipartFile("file", "cover.PNG", "image/png", new byte[]{1, 2, 3});
        when(keyGenerator.generate("events/27/cover", "cover.PNG")).thenReturn("events/27/cover/new.png");

        service.uploadCoverImage(EVENT_ID, USER_ID, file);

        assertThat(event.getCoverImageKey()).isEqualTo("events/27/cover/new.png");
        InOrder order = inOrder(accessPolicy, storageExternal, eventRepository);
        order.verify(accessPolicy).assertCanEdit(event, USER_ID);
        order.verify(storageExternal).putObject("events/27/cover/new.png", file);
        order.verify(eventRepository).save(event);
        order.verify(storageExternal).deleteObject("events/27/cover/old.jpg");
    }

    @Test
    void uploadRejectsMissingOrEmptyFile() {
        assertThatThrownBy(() -> service.uploadCoverImage(EVENT_ID, USER_ID, null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Nenhum arquivo enviado.");
        assertThatThrownBy(() -> service.uploadCoverImage(EVENT_ID, USER_ID,
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])))
                .isInstanceOf(DomainException.class)
                .hasMessage("Nenhum arquivo enviado.");

        verifyNoInteractions(storageExternal, keyGenerator);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void uploadRejectsUnsupportedContentType() {
        var file = new MockMultipartFile("file", "document.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> service.uploadCoverImage(EVENT_ID, USER_ID, file))
                .isInstanceOf(DomainException.class)
                .hasMessage("Tipo de arquivo não suportado. Utilize PNG, JPEG ou WebP.");

        verifyNoInteractions(storageExternal, keyGenerator);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void uploadAllowsJpegAndWebpTypes() {
        when(keyGenerator.generate("events/27/cover", "cover"))
                .thenReturn("events/27/cover/jpeg", "events/27/cover/webp");
        for (String contentType : new String[]{"image/jpeg", "image/webp"}) {
            var file = new MockMultipartFile("file", "cover", contentType, new byte[]{1});

            service.uploadCoverImage(EVENT_ID, USER_ID, file);

            verify(storageExternal).putObject("events/27/cover/" + contentType.substring("image/".length()), file);
        }
        verify(eventRepository, times(2)).save(event);
    }

    @Test
    void getCoverImageReturnsEmptyWithoutCallingStorageWhenNoKeyIsSet() {
        assertThat(service.getCoverImage(EVENT_ID, null)).isEmpty();

        verify(accessPolicy).assertCanView(event, null);
        verifyNoInteractions(storageExternal);
    }

    @Test
    void getCoverImageDelegatesToStorageWhenKeyExists() {
        event.setCoverImageKey("events/27/cover/cover.webp");
        var storedFile = new StorageExternal.StoredFile(null, "image/webp");
        when(storageExternal.getObject(event.getCoverImageKey())).thenReturn(Optional.of(storedFile));

        assertThat(service.getCoverImage(EVENT_ID, USER_ID)).contains(storedFile);
        verify(accessPolicy).assertCanView(event, USER_ID);
        verify(storageExternal).getObject("events/27/cover/cover.webp");
    }

    @Test
    void removeCoverImageReturnsFalseWhenNoImageExists() {
        assertThat(service.removeCoverImage(EVENT_ID, USER_ID)).isFalse();

        verify(accessPolicy).assertCanEdit(event, USER_ID);
        verifyNoInteractions(storageExternal);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void removeCoverImageDeletesStorageAndClearsDatabaseKey() {
        event.setCoverImageKey("events/27/cover/current.jpg");

        assertThat(service.removeCoverImage(EVENT_ID, USER_ID)).isTrue();

        assertThat(event.getCoverImageKey()).isNull();
        InOrder order = inOrder(storageExternal, eventRepository);
        order.verify(storageExternal).deleteObject("events/27/cover/current.jpg");
        order.verify(eventRepository).save(event);
    }

    @Test
    void operationsFailWhenEventDoesNotExist() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadCoverImage(EVENT_ID, USER_ID,
                new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1})))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento não encontrado.");
        assertThatThrownBy(() -> service.getCoverImage(EVENT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.removeCoverImage(EVENT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(accessPolicy, storageExternal, keyGenerator);
    }
}
