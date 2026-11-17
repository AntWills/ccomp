package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsImageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NewsImageControllerTest {
    private static final Long NEWS_ID = 18L;
    private static final UUID USER_ID = UUID.fromString("d6a0588a-936b-49b5-9e81-ce876a151f0a");
    @Mock private NewsImageService imageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NewsImageController(imageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(USER_ID.toString()).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void uploadPassesFileAndUser() throws Exception {
        mockMvc.perform(multipart("/api/news/{newsId}/images/cover", NEWS_ID)
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1})))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response").value("Imagem de capa atualizada com sucesso."));
        verify(imageService).uploadCoverImage(eq(NEWS_ID), eq(USER_ID), any());
    }

    @Test
    void uploadMapsValidationErrorAndRequiresFile() throws Exception {
        doThrow(new DomainException("Tipo de arquivo não suportado."))
                .when(imageService).uploadCoverImage(eq(NEWS_ID), eq(USER_ID), any());
        mockMvc.perform(multipart("/api/news/{newsId}/images/cover", NEWS_ID)
                        .file(new MockMultipartFile("file", "cover.gif", "image/gif", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.messages[0]").value("Tipo de arquivo não suportado."));
        mockMvc.perform(multipart("/api/news/{newsId}/images/cover", NEWS_ID)).andExpect(status().isBadRequest());
    }

    @Test
    void getImageSupportsAnonymousNoContentAndStoredContentType() throws Exception {
        SecurityContextHolder.clearContext();
        when(imageService.getCoverImage(NEWS_ID, null)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/news/{newsId}/images/cover", NEWS_ID)).andExpect(status().isNoContent());
        verify(imageService).getCoverImage(NEWS_ID, null);

        var file = new StorageExternal.StoredFile(new ByteArrayResource(new byte[]{2, 3}), "image/webp");
        when(imageService.getCoverImage(NEWS_ID, USER_ID)).thenReturn(Optional.of(file));
        authenticate();
        mockMvc.perform(get("/api/news/{newsId}/images/cover", NEWS_ID)).andExpect(status().isOk())
                .andExpect(content().contentType("image/webp")).andExpect(content().bytes(new byte[]{2, 3}));
    }

    @Test
    void getImageUsesBinaryFallbackAndRemoveReturnsExpectedStatuses() throws Exception {
        when(imageService.getCoverImage(NEWS_ID, USER_ID)).thenReturn(Optional.of(
                new StorageExternal.StoredFile(new ByteArrayResource(new byte[]{1}), null)));
        mockMvc.perform(get("/api/news/{newsId}/images/cover", NEWS_ID)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        when(imageService.removeCoverImage(NEWS_ID, USER_ID)).thenReturn(true, false);
        mockMvc.perform(delete("/api/news/{newsId}/images/cover", NEWS_ID)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/news/{newsId}/images/cover", NEWS_ID)).andExpect(status().isNotFound());
    }

    private void authenticate() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(USER_ID.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }
}
