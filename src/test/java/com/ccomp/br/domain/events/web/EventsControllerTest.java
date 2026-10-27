package com.ccomp.br.domain.events.web;

import com.ccomp.br.domain.events.core.application.EventsServices;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.dto.CreateEventDTO;
import com.ccomp.br.domain.events.core.dto.EventsFilterRequest;
import com.ccomp.br.domain.events.core.dto.UpdateEventDTO;
import com.ccomp.br.domain.events.core.dto.EventListItemDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.web.EventsController;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.GlobalExceptionHandler;
import com.ccomp.br.shared.utils.CursorPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventsControllerTest {
    private static final UUID USER_ID = UUID.fromString("79e347e8-0245-4ff0-b85e-3db1922a0378");

    @Mock private EventsServices eventsServices;
    @InjectMocks private EventsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = jwt(USER_ID);
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                jwt, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    void getByIdReturnsEventForAuthenticatedUser() {
        Long eventId = 9L;
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        EventDTO event = mock(EventDTO.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(eventsServices.getById(eventId, userId)).thenReturn(Optional.of(event));

        var response = controller.getById(eventId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(event);
    }

    @Test
    void getByIdReturnsNotFoundForUnknownEvent() {
        when(eventsServices.getById(9L, null)).thenReturn(Optional.empty());

        assertThat(controller.getById(9L, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBySlugReturnsNotFoundWhenServiceHasNoPublishedEvent() {
        when(eventsServices.getBySlug("draft-event")).thenReturn(Optional.empty());

        assertThat(controller.getBySlug("draft-event").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatusDelegatesStatusAndAuthenticatedSubject() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        MessageResponse expected = new MessageResponse("Status do evento alterado para: PUBLISHED");
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(eventsServices.updateEventStatus(9L, EnumEventStatus.PUBLISHED, userId)).thenReturn(expected);

        var response = controller.updateEventStatus(9L, EnumEventStatus.PUBLISHED, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void mvcGetByIdPassesEventAndAuthenticatedSubject() throws Exception {
        EventDTO event = eventDto();
        when(eventsServices.getById(9L, USER_ID)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/events/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        verify(eventsServices).getById(9L, USER_ID);
    }

    @Test
    void mvcGetBySlugReturnsNotFoundForUnavailableSlug() throws Exception {
        when(eventsServices.getBySlug("draft-event")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/slug/draft-event"))
                .andExpect(status().isNotFound());
    }

    @Test
    void mvcSearchBindsSnakeCaseSearchTermAndPagingParameters() throws Exception {
        when(eventsServices.searchEventsWithFilters(any(EventsFilterRequest.class), eq("page-two"), eq(3)))
                .thenReturn(new CursorPage<EventListItemDTO>(List.of(), "next", null));

        mockMvc.perform(post("/api/events/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"ACADEMIC_EDUCATIONAL","format":"IN_PERSON","search_term":"confernece"}
                                """)
                        .param("nextCursor", "page-two")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.next_cursor").value("next"));

        var filterCaptor = org.mockito.ArgumentCaptor.forClass(EventsFilterRequest.class);
        verify(eventsServices).searchEventsWithFilters(filterCaptor.capture(), eq("page-two"), eq(3));
        assertThat(filterCaptor.getValue().searchTerm()).isEqualTo("confernece");
        assertThat(filterCaptor.getValue().searchTermOpt()).contains("confernece");
    }

    @Test
    void mvcCreateReturnsCreatedAndUsesAuthenticatedSubject() throws Exception {
        var request = new CreateEventDTO("Evento de teste", null, null, null, null);
        var expected = eventDto();
        when(eventsServices.create(eq(USER_ID), any(CreateEventDTO.class))).thenReturn(expected);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Evento de teste","category":"ACADEMIC_EDUCATIONAL","format":"IN_PERSON"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(CreateEventDTO.class);
        verify(eventsServices).create(eq(USER_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().title()).isEqualTo(request.title());
    }

    @Test
    void mvcUpdatePassesPartialRequestAndAuthenticatedSubject() throws Exception {
        when(eventsServices.update(any(UpdateEventDTO.class), eq(9L), eq(USER_ID))).thenReturn(eventDto());

        mockMvc.perform(patch("/api/events/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Novo evento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(UpdateEventDTO.class);
        verify(eventsServices).update(requestCaptor.capture(), eq(9L), eq(USER_ID));
        assertThat(requestCaptor.getValue().title()).isEqualTo("Novo evento");
    }

    @Test
    void mvcUpdateStatusAndDeleteUseAuthenticatedSubject() throws Exception {
        when(eventsServices.updateEventStatus(9L, EnumEventStatus.CANCELED, USER_ID))
                .thenReturn(new MessageResponse("cancelado"));

        mockMvc.perform(patch("/api/events/9/status/CANCELED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("cancelado"));
        mockMvc.perform(delete("/api/events/9"))
                .andExpect(status().isNoContent());

        verify(eventsServices).updateEventStatus(9L, EnumEventStatus.CANCELED, USER_ID);
        verify(eventsServices).delete(9L, USER_ID);
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(userId.toString()).build();
    }

    private static EventDTO eventDto() {
        return new EventDTO(9L, "Evento", "evento", null, null, null,
                null, null, null, EnumEventStatus.PUBLISHED,
                LocalDateTime.parse("2026-11-10T10:00:00"), null, null, null, null, false, null,
                USER_ID, null, null, null);
    }
}
