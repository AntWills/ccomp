package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.web.EventsMeController;
import com.ccomp.br.shared.exceptions.GlobalExceptionHandler;
import com.ccomp.br.shared.utils.CursorPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventsMeControllerTest {
    private static final UUID USER_ID = UUID.fromString("b95adce0-a0aa-42d9-a8a4-8f1a6ea3aa1a");

    @Mock private EventsServices eventsServices;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(USER_ID.toString()).build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mockMvc = MockMvcBuilders.standaloneSetup(new EventsMeController(eventsServices))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCreatedEventsUsesAuthenticatedOwnerAndDefaultPaging() throws Exception {
        when(eventsServices.findAllByOwnerId(USER_ID, null, 10)).thenReturn(page("created-cursor"));

        mockMvc.perform(get("/api/events/me/created"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.next_cursor").value("created-cursor"))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(eventsServices).findAllByOwnerId(USER_ID, null, 10);
    }

    @Test
    void getCreatedEventsPassesCursorAndPageSize() throws Exception {
        when(eventsServices.findAllByOwnerId(USER_ID, "abc123", 4)).thenReturn(page(null));

        mockMvc.perform(get("/api/events/me/created")
                        .param("nextCursor", "abc123")
                        .param("pageSize", "4"))
                .andExpect(status().isOk());

        verify(eventsServices).findAllByOwnerId(USER_ID, "abc123", 4);
    }

    @Test
    void getSubscriptionsUsesAuthenticatedParticipant() throws Exception {
        when(eventsServices.findAllSubscriptions(USER_ID, "sub-cursor", 7)).thenReturn(page("next"));

        mockMvc.perform(get("/api/events/me/subscriptions")
                        .param("nextCursor", "sub-cursor")
                        .param("pageSize", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.next_cursor").value("next"));

        verify(eventsServices).findAllSubscriptions(USER_ID, "sub-cursor", 7);
    }

    @Test
    void getEditorEventsUsesAuthenticatedUser() throws Exception {
        when(eventsServices.findMyEditableEvents(USER_ID, null, 10)).thenReturn(page(null));

        mockMvc.perform(get("/api/events/me/editors"))
                .andExpect(status().isOk());

        verify(eventsServices).findMyEditableEvents(USER_ID, null, 10);
    }

    @Test
    void invalidPageSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/me/created").param("pageSize", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(eventsServices);
    }

    private CursorPage<EventListItemDTO> page(String nextCursor) {
        return new CursorPage<>(List.of(), nextCursor, null);
    }
}
