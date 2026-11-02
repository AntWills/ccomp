package com.ccomp.br.domain.events.materials.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.materials.dto.CreateMaterialLinkRequest;
import com.ccomp.br.domain.events.materials.dto.MaterialCursor;
import com.ccomp.br.domain.events.materials.enums.EnumMaterialType;
import com.ccomp.br.domain.events.materials.persistence.EventMaterial;
import com.ccomp.br.domain.events.materials.persistence.EventMaterialDslRepository;
import com.ccomp.br.domain.events.materials.persistence.EventMaterialRepository;
import com.ccomp.br.domain.events.materials.utils.MaterialMapper;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.utils.CursorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMaterialsServiceTest {
    private static final Long EVENT_ID = 15L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private EventRepository events;

    @Mock
    private EventMaterialRepository materials;

    @Mock
    private EventMaterialDslRepository materialDsl;

    @Mock
    private EventAccessPolicy access;

    @Mock
    private StorageExternal storage;

    @Mock
    private StorageKeyGenerator keyGenerator;

    private EventMaterialsService service;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new EventMaterialsService(
                events, materials, access, storage, materialDsl, keyGenerator, new MaterialMapper());
        event = Event.builder().id(EVENT_ID).build();
        when(events.findById(EVENT_ID)).thenReturn(Optional.of(event));
    }

    @Test
    void addsHttpLinkAfterEditAuthorization() {
        when(materials.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.addLink(
                EVENT_ID,
                USER_ID,
                new CreateMaterialLinkRequest("  Guia ", null, "https://example.org/guia"));

        assertThat(result.type()).isEqualTo(EnumMaterialType.LINK);
        assertThat(result.title()).isEqualTo("Guia");
        assertThat(result.url()).isEqualTo("https://example.org/guia");
        verify(access).assertCanEdit(event, USER_ID);
    }

    @Test
    void rejectsNonHttpLink() {
        assertThatThrownBy(() -> service.addLink(
                EVENT_ID,
                USER_ID,
                new CreateMaterialLinkRequest("Link", null, "javascript:alert(1)")))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(materials);
    }

    @Test
    void listsMaterialsUsingDecodedCursorAndRequestsOneExtraRow() {
        MaterialCursor cursor = new MaterialCursor(LocalDateTime.now(), 12L);
        String encodedCursor = CursorUtils.encode(cursor);

        service.list(EVENT_ID, null, encodedCursor, 10);

        verify(materialDsl).findAllByEventIdWithCursor(EVENT_ID, cursor, 11);
    }

    @Test
    void uploadsAllowedPdfAndSavesItsMetadata() {
        var file = new MockMultipartFile(
                "file", "manual.pdf", "application/pdf", "%PDF-1.7".getBytes());
        when(keyGenerator.generate("events/15/materials", "material.pdf"))
                .thenReturn("events/15/materials/a.pdf");
        when(materials.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.upload(EVENT_ID, USER_ID, "Manual", null, file);

        assertThat(response.originalFileName()).isEqualTo("manual.pdf");
        assertThat(response.sizeBytes()).isEqualTo(8);
        verify(storage).putObject("events/15/materials/a.pdf", file);
    }

    @Test
    void rejectsFileWithMismatchedContentTypeSignature() {
        var file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "not a pdf".getBytes());

        assertThatThrownBy(() -> service.upload(EVENT_ID, USER_ID, "Arquivo", null, file))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(storage, keyGenerator, materials);
    }

    @Test
    void rejectsUnsupportedFileBeforeStorage() {
        var file = new MockMultipartFile("file", "script.html", "text/html", new byte[]{1});

        assertThatThrownBy(() -> service.upload(EVENT_ID, USER_ID, "Arquivo", null, file))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(storage, keyGenerator, materials);
    }

    @Test
    void editPermissionIsRequiredBeforeUpload() {
        doThrow(new AccessDeniedException("negado"))
                .when(access).assertCanEdit(event, USER_ID);
        var file = new MockMultipartFile("file", "manual.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.upload(EVENT_ID, USER_ID, "Manual", null, file))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(storage, keyGenerator, materials);
    }
}
