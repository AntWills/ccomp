package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.core.application.EventImageService;
import com.ccomp.br.domain.events.core.web.EventImageController;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventImageControllerTest {
    private static final Long EVENT_ID = 42L;
    private static final UUID USER_ID = UUID.fromString("d6a0588a-936b-49b5-9e81-ce876a151f0a");

    @Mock private EventImageService eventImageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventImageController(eventImageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadCoverImageReturnsSuccessAndPassesFileAndSubject() throws Exception {
        var file = new MockMultipartFile("file", "cover.webp", "image/webp", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/events/{eventId}/images/cover", EVENT_ID).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Imagem de capa atualizada com sucesso."));

        verify(eventImageService).uploadCoverImage(eq(EVENT_ID), eq(USER_ID), any());
    }

    @Test
    void uploadCoverImageMapsUnsupportedTypeToBadRequest() throws Exception {
        var file = new MockMultipartFile("file", "cover.gif", "image/gif", new byte[]{1});
        doThrow(new DomainException("Tipo de arquivo não suportado."))
                .when(eventImageService).uploadCoverImage(eq(EVENT_ID), eq(USER_ID), any());

        mockMvc.perform(multipart("/api/events/{eventId}/images/cover", EVENT_ID).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Tipo de arquivo não suportado."));
    }

    @Test
    void uploadCoverImageRequiresMultipartFile() throws Exception {
        mockMvc.perform(multipart("/api/events/{eventId}/images/cover", EVENT_ID))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventImageService);
    }

    @Test
    void getCoverImageReturnsNoContentWhenImageIsMissingAndPassesAnonymousIdentity() throws Exception {
        SecurityContextHolder.clearContext();
        when(eventImageService.getCoverImage(EVENT_ID, null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/{eventId}/images/cover", EVENT_ID))
                .andExpect(status().isNoContent());

        verify(eventImageService).getCoverImage(EVENT_ID, null);
    }

    @Test
    void getCoverImageReturnsBytesAndStoredContentType() throws Exception {
        var resource = new ByteArrayResource(new byte[]{10, 20, 30});
        when(eventImageService.getCoverImage(EVENT_ID, USER_ID))
                .thenReturn(Optional.of(new StorageExternal.StoredFile(resource, "image/webp")));

        mockMvc.perform(get("/api/events/{eventId}/images/cover", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(new byte[]{10, 20, 30}));
    }

    @Test
    void getCoverImageUsesBinaryFallbackWhenStoredTypeIsMissing() throws Exception {
        when(eventImageService.getCoverImage(EVENT_ID, USER_ID))
                .thenReturn(Optional.of(new StorageExternal.StoredFile(new ByteArrayResource(new byte[]{1}), null)));

        mockMvc.perform(get("/api/events/{eventId}/images/cover", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void removeCoverImageReturnsNoContentWhenRemoved() throws Exception {
        when(eventImageService.removeCoverImage(EVENT_ID, USER_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/events/{eventId}/images/cover", EVENT_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeCoverImageReturnsNotFoundWhenNoCoverWasSet() throws Exception {
        when(eventImageService.removeCoverImage(EVENT_ID, USER_ID)).thenReturn(false);

        mockMvc.perform(delete("/api/events/{eventId}/images/cover", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.response").value("Este evento não possui imagem de capa definida."));
    }

    private void authenticate(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
