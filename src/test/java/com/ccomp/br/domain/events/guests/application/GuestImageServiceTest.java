package com.ccomp.br.domain.events.guests.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestImageServiceTest {

    private static final Long EVENT_ID = 12L;
    private static final Long GUEST_ID = 35L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private EventAccessPolicy eventAccessPolicy;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventGuestRepository eventGuestRepository;

    @Mock
    private StorageExternal storageExternal;

    @Mock
    private StorageKeyGenerator storageKeyGenerator;

    private GuestImageService guestImageService;
    private Event event;
    private EventGuest guest;

    @BeforeEach
    void setUp() {
        guestImageService = new GuestImageService(
                eventAccessPolicy,
                eventRepository,
                eventGuestRepository,
                storageExternal,
                storageKeyGenerator
        );

        event = Event.builder().id(EVENT_ID).build();
        guest = EventGuest.builder()
                .id(GUEST_ID)
                .event(event)
                .name("Ada Lovelace")
                .imageKey("events/12/guests/35/old.webp")
                .build();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventGuestRepository.findByIdAndEventId(GUEST_ID, EVENT_ID)).thenReturn(Optional.of(guest));
    }

    @Test
    void uploadStoresImageAndDeletesPreviousObject() {
        var file = new MockMultipartFile("file", "portrait.webp", "image/webp", new byte[]{1, 2, 3});
        when(storageKeyGenerator.generate("events/12/guests/35", "portrait.webp"))
                .thenReturn("events/12/guests/35/new.webp");

        guestImageService.uploadImage(EVENT_ID, GUEST_ID, USER_ID, file);

        verify(eventAccessPolicy).assertCanEdit(event, USER_ID);
        verify(storageExternal).putObject("events/12/guests/35/new.webp", file);
        verify(eventGuestRepository).save(guest);
        verify(storageExternal).deleteObject("events/12/guests/35/old.webp");
        assertThat(guest.getImageKey()).isEqualTo("events/12/guests/35/new.webp");
    }

    @Test
    void uploadRejectsUnsupportedContentTypeBeforeWritingToStorage() {
        var file = new MockMultipartFile("file", "portrait.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> guestImageService.uploadImage(EVENT_ID, GUEST_ID, USER_ID, file))
                .isInstanceOf(DomainException.class)
                .hasMessage("Tipo de arquivo não suportado. Utilize PNG, JPEG ou WebP.");

        verifyNoInteractions(storageExternal);
    }

    @Test
    void getImageReturnsStoredFileForVisibleEvent() {
        StorageExternal.StoredFile storedFile = new StorageExternal.StoredFile(
                new ByteArrayResource(new byte[]{1}),
                "image/webp"
        );
        when(storageExternal.getObject("events/12/guests/35/old.webp"))
                .thenReturn(Optional.of(storedFile));

        assertThat(guestImageService.getImage(EVENT_ID, GUEST_ID, null))
                .containsSame(storedFile);

        verify(eventAccessPolicy).assertCanView(event, null);
    }

    @Test
    void removeImageClearsKeyAndDeletesStoredObject() {
        assertThat(guestImageService.removeImage(EVENT_ID, GUEST_ID, USER_ID)).isTrue();

        verify(eventAccessPolicy).assertCanEdit(event, USER_ID);
        verify(storageExternal).deleteObject("events/12/guests/35/old.webp");
        verify(eventGuestRepository).save(guest);
        assertThat(guest.getImageKey()).isNull();
    }

    @Test
    void imageLookupRejectsGuestFromDifferentEvent() {
        when(eventGuestRepository.findByIdAndEventId(GUEST_ID, EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestImageService.getImage(EVENT_ID, GUEST_ID, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Convidado não encontrado neste evento.");
    }
}
