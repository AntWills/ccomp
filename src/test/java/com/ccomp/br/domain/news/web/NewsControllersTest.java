package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsApplication;
import com.ccomp.br.domain.news.application.NewsEditorServices;
import com.ccomp.br.domain.news.dto.NewsCursor;
import com.ccomp.br.domain.news.dto.NewsItem;
import com.ccomp.br.domain.news.dto.NewsResponse;
import com.ccomp.br.domain.news.dto.NewsSearchFilter;
import com.ccomp.br.domain.news.dto.UserNewsResponseDTO;
import com.ccomp.br.shared.utils.CursorPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NewsControllersTest {
    private static final UUID USER_ID = UUID.fromString("51d70b9f-f904-48c0-a67b-61fa5f80ed27");
    @Mock NewsApplication newsApplication;
    @Mock NewsEditorServices editorServices;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject(USER_ID.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new NewsController(newsApplication), new NewsEditorController(editorServices))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void publicSlugLookupReturnsNotFoundWhenNoPublishedNews() throws Exception {
        when(newsApplication.getBySlug("missing-news")).thenReturn(Optional.empty());
        mvc.perform(get("/api/news/missing-news")).andExpect(status().isNotFound());
    }

    @Test
    void publicSearchMapsFeaturedFilterAndCursor() throws Exception {
        when(newsApplication.searchNewsWithFilters(new NewsSearchFilter(true), "next", 3))
                .thenReturn(new CursorPage<>(List.<NewsItem>of(), null, null));
        mvc.perform(post("/api/news/search?nextCursor=next&pageSize=3")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"featured\":true}"))
                .andExpect(status().isOk());
        verify(newsApplication).searchNewsWithFilters(new NewsSearchFilter(true), "next", 3);
    }

    @Test
    void myNewsForwardsAuthenticatedUser() throws Exception {
        when(editorServices.listNewsForUser(USER_ID)).thenReturn(new UserNewsResponseDTO(List.of(), List.of()));
        mvc.perform(get("/api/news/me")).andExpect(status().isOk());
        verify(editorServices).listNewsForUser(USER_ID);
    }
}
