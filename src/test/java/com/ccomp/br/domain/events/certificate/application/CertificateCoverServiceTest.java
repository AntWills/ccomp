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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateCoverServiceTest {
    private static final UUID USER_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");
    private static final String STORAGE_KEY = "events/10/certificate-cover-template/fundo.png";

    @Mock
    private EventCoverTemplateRepository coverTemplateRepository;

    @Mock
    private CertificateFetcher certificateFetcher;

    @Mock
    private EventCache eventCache;

    @Mock
    private EventAccessPolicy accessPolicy;

    @Mock
    private StorageKeyGenerator storageKeyGenerator;

    @Mock
    private StorageExternal storageExternal;

    private CertificateCoverService createService() {
        return new CertificateCoverService(coverTemplateRepository, certificateFetcher,
                eventCache, accessPolicy, storageKeyGenerator, storageExternal);
    }

    // --- Remoção ---

    @Test
    void deleteCoverTemplate_eventNotFound_throws() {
        when(eventCache.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService().deleteCoverTemplate(10L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento não encontrado.");
    }

    @Test
    void deleteCoverTemplate_accessDenied_throwsAndKeepsCover() {
        stubEventWithEditPermission(false);

        assertThatThrownBy(() -> createService().deleteCoverTemplate(10L, USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("O usuário não tem acesso a este recurso.");

        verify(coverTemplateRepository, never()).delete(any());
        verify(storageExternal, never()).deleteObject(anyString());
    }

    @Test
    void deleteCoverTemplate_withoutSavedCover_returnsFalseWithoutTouchingStorage() {
        stubEventWithEditPermission(true);
        when(coverTemplateRepository.findByEventId(10L)).thenReturn(Optional.empty());

        boolean removed = createService().deleteCoverTemplate(10L, USER_ID);

        assertThat(removed).isFalse();
        verify(storageExternal, never()).deleteObject(anyString());
    }

    @Test
    void deleteCoverTemplate_savedCover_removesRowAndStorageObject() {
        EventCoverTemplate savedCover = savedCover();
        stubEventWithEditPermission(true);
        when(coverTemplateRepository.findByEventId(10L)).thenReturn(Optional.of(savedCover));

        boolean removed = createService().deleteCoverTemplate(10L, USER_ID);

        assertThat(removed).isTrue();
        verify(coverTemplateRepository).delete(savedCover);
        verify(storageExternal).deleteObject(STORAGE_KEY);
    }

    // --- Upload ---

    @Test
    void uploadCoverTemplate_unsupportedType_throwsBeforeStorage() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        doReturn("not an image".getBytes()).when(file).getBytes();
        stubEventWithEditPermission(true);

        assertThatThrownBy(() -> createService().uploadCoverTemplate(10L, USER_ID, file))
                .isInstanceOf(DomainException.class)
                .hasMessage("Tipo de arquivo não suportado. Utilize PNG, JPEG ou Webp.");

        verify(storageExternal, never()).putObject(anyString(), any());
    }

    @Test
    void uploadCoverTemplate_accessDenied_throws() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        stubEventWithEditPermission(false);

        assertThatThrownBy(() -> createService().uploadCoverTemplate(10L, USER_ID, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("O usuário não tem acesso a este recurso.");

        verify(storageExternal, never()).putObject(anyString(), any());
    }

    @Test
    void uploadCoverTemplate_firstCover_persistsKeyAndUploadsObject() throws Exception {
        MultipartFile file = pngFile();
        stubEventWithEditPermission(true);
        when(storageKeyGenerator.generate(anyString(), any())).thenReturn(STORAGE_KEY);
        when(coverTemplateRepository.findByEventId(10L)).thenReturn(Optional.empty());
        when(coverTemplateRepository.save(any(EventCoverTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        createService().uploadCoverTemplate(10L, USER_ID, file);

        verify(storageExternal).putObject(eq(STORAGE_KEY), any(byte[].class), eq("image/png"));

        ArgumentCaptor<EventCoverTemplate> captor =
                ArgumentCaptor.forClass(EventCoverTemplate.class);
        verify(coverTemplateRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(10L);
        assertThat(captor.getValue().getCoverTemplateKey()).isEqualTo(STORAGE_KEY);
    }

    @Test
    void uploadCoverTemplate_replacingCover_deletesPreviousObject() throws Exception {
        MultipartFile file = pngFile();
        String replacementKey = "events/10/certificate-cover-template/novo.png";
        stubEventWithEditPermission(true);
        when(storageKeyGenerator.generate(anyString(), any())).thenReturn(replacementKey);
        when(coverTemplateRepository.findByEventId(10L)).thenReturn(Optional.of(savedCover()));
        when(coverTemplateRepository.save(any(EventCoverTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        createService().uploadCoverTemplate(10L, USER_ID, file);

        verify(storageExternal).deleteObject(STORAGE_KEY);
        verify(storageExternal).putObject(eq(replacementKey), any(byte[].class), eq("image/png"));
    }

    private void stubEventWithEditPermission(boolean canEdit) {
        when(eventCache.findById(10L)).thenReturn(Optional.of(eventDto()));
        when(accessPolicy.canEdit(any(), eq(USER_ID))).thenReturn(canEdit);
    }

    private static EventCoverTemplate savedCover() {
        return EventCoverTemplate.builder()
                .id(1L)
                .eventId(10L)
                .coverTemplateKey(STORAGE_KEY)
                .build();
    }

    private static MultipartFile pngFile() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("fundo.png");
        doReturn(pngBytes()).when(file).getBytes();
        return file;
    }

    private static byte[] pngBytes() {
        try {
            var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var output = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static EventDTO eventDto() {
        return new EventDTO(10L, "Semanas de Tecnologia", "slug", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
