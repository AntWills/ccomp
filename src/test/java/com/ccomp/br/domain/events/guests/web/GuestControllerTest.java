package com.ccomp.br.domain.events.guests.web;

import com.ccomp.br.domain.events.guests.application.GuestImageService;
import com.ccomp.br.domain.events.guests.application.GuestService;
import com.ccomp.br.domain.events.guests.dto.CreateGuestDTO;
import com.ccomp.br.domain.events.guests.dto.EventGuestDTO;
import com.ccomp.br.domain.events.guests.dto.UpdateGuestProfileDTO;
import com.ccomp.br.domain.events.guests.enums.EnumGuestStatus;
import com.ccomp.br.domain.events.guests.enums.EnumGuestVisibility;
import com.ccomp.br.domain.events.guests.persistence.EventGuest;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.GlobalExceptionHandler;
import com.ccomp.br.shared.utils.CursorPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GuestControllerTest {

    private static final Long EVENT_ID = 12L;
    private static final Long GUEST_ID = 35L;
    private static final UUID USER_ID = UUID.fromString("fe964c8a-54c8-42de-a39e-7632f47e2444");

    @Mock
    private GuestService guestService;

    @Mock
    private GuestImageService guestImageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticate(USER_ID);

        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        mockMvc = MockMvcBuilders.standaloneSetup(new GuestController(guestService, guestImageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new ResourceHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createGuestDeserializesProfileAndActivityId() throws Exception {
        EventGuest guest = mock(EventGuest.class);
        when(guest.getId()).thenReturn(GUEST_ID);
        when(guestService.createGuest(eq(EVENT_ID), eq(USER_ID), any(CreateGuestDTO.class))).thenReturn(guest);

        mockMvc.perform(post("/api/events/{eventId}/guests", EVENT_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ada Lovelace",
                                  "description": "Palestrante convidada",
                                  "activity_id": 78
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(GUEST_ID));

        ArgumentCaptor<CreateGuestDTO> request = ArgumentCaptor.forClass(CreateGuestDTO.class);
        verify(guestService).createGuest(eq(EVENT_ID), eq(USER_ID), request.capture());
        assertThat(request.getValue().name()).isEqualTo("Ada Lovelace");
        assertThat(request.getValue().description()).isEqualTo("Palestrante convidada");
        assertThat(request.getValue().activityId()).isEqualTo(78L);
    }

    @Test
    void updateGuestProfileDeserializesTextFields() throws Exception {
        mockMvc.perform(patch("/api/events/{eventId}/guests/{guestId}/profile", EVENT_ID, GUEST_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Grace Hopper",
                                  "description": "Palestra de encerramento"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Dados do convidado atualizados com sucesso."));

        ArgumentCaptor<UpdateGuestProfileDTO> request = ArgumentCaptor.forClass(UpdateGuestProfileDTO.class);
        verify(guestService).updateGuestProfile(eq(EVENT_ID), eq(GUEST_ID), eq(USER_ID), request.capture());
        assertThat(request.getValue().name()).isEqualTo("Grace Hopper");
        assertThat(request.getValue().description()).isEqualTo("Palestra de encerramento");
    }

    @Test
    void linkAccountDeserializesEmailRatherThanUserId() throws Exception {
        mockMvc.perform(patch("/api/events/{eventId}/guests/{guestId}/account", EVENT_ID, GUEST_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "ada@example.com",
                                  "activity_id": 78
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(guestService).linkAccount(eq(EVENT_ID), eq(GUEST_ID), eq(USER_ID),
                org.mockito.ArgumentMatchers.eq("ada@example.com"), org.mockito.ArgumentMatchers.eq(78L));
    }

    @Test
    void guestListSerializesPublicProfileWithoutAccountIdOrEmail() throws Exception {
        SecurityContextHolder.clearContext();
        EventGuestDTO guest = new EventGuestDTO(
                GUEST_ID,
                EVENT_ID,
                "Ada Lovelace",
                "Palestrante",
                "events/12/guests/35/ada.webp",
                EnumGuestStatus.CONFIRMED,
                EnumGuestVisibility.PUBLIC,
                null
        );
        when(guestService.searchEventGuests(EVENT_ID, null, null, 20))
                .thenReturn(new CursorPage<>(List.of(guest), null, null));

        mockMvc.perform(get("/api/events/{eventId}/guests", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.content[0].description").value("Palestrante"))
                .andExpect(jsonPath("$.content[0].image_key").value("events/12/guests/35/ada.webp"))
                .andExpect(jsonPath("$.content[0].event_id").value(EVENT_ID))
                .andExpect(jsonPath("$.content[0].user_id").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
    }

    @Test
    void uploadGuestImageAcceptsMultipartAndUsesRequestIdentity() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portrait.webp",
                "image/webp",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/events/{eventId}/guests/{guestId}/image", EVENT_ID, GUEST_ID)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Imagem do convidado atualizada com sucesso."));

        verify(guestImageService).uploadImage(eq(EVENT_ID), eq(GUEST_ID), eq(USER_ID), any());
    }

    @Test
    void getGuestImageSerializesStoredBytesWithContentType() throws Exception {
        SecurityContextHolder.clearContext();
        when(guestImageService.getImage(EVENT_ID, GUEST_ID, null))
                .thenReturn(java.util.Optional.of(new StorageExternal.StoredFile(
                        new ByteArrayResource(new byte[]{4, 5, 6}),
                        "image/webp"
                )));

        mockMvc.perform(get("/api/events/{eventId}/guests/{guestId}/image", EVENT_ID, GUEST_ID))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/webp"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(new byte[]{4, 5, 6}));
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
