package com.ccomp.br.domain.events.credential.web;

import com.ccomp.br.domain.events.credential.application.EventCredentialService;
import com.ccomp.br.domain.events.credential.dto.CredentialResponse;
import com.ccomp.br.domain.events.credential.dto.CredentialSearchItem;
import com.ccomp.br.domain.events.credential.dto.CredentialSummaryResponse;
import com.ccomp.br.domain.events.credential.dto.CredentialTokenResponse;
import com.ccomp.br.shared.exceptions.GlobalExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
class EventCredentialControllerTest {
    private static final Long EVENT_ID = 41L;
    private static final UUID USER_ID = UUID.fromString("589a96af-d365-45d6-8942-b6ee731a0e3d");

    @Mock private EventCredentialService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "RS256").subject(USER_ID.toString()).build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        mockMvc = MockMvcBuilders.standaloneSetup(new EventCredentialController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void manualCredentialingBindsRequestAndAuthenticatedUser() throws Exception {
        when(service.manual(eq(EVENT_ID), eq(USER_ID), any()))
                .thenReturn(new CredentialResponse("CREDENTIALED", 91L, 81L, "CHECKED_IN",
                        LocalDateTime.parse("2026-11-05T09:00:00"), true, "Ana Souza", "a***@example.org"));

        mockMvc.perform(post("/api/events/{eventId}/credentials/manual", EVENT_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"enrollmentId":81,"station":"A"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("CREDENTIALED"))
                .andExpect(jsonPath("$.credentialId").value(91))
                .andExpect(jsonPath("$.emailMasked").value("a***@example.org"));

        verify(service).manual(eq(EVENT_ID), eq(USER_ID), any());
    }

    @Test
    void manualCredentialingRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/events/{eventId}/credentials/manual", EVENT_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"station":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summaryReturnsAggregateCounts() throws Exception {
        when(service.summary(EVENT_ID, USER_ID)).thenReturn(new CredentialSummaryResponse(120, 44, 76));

        mockMvc.perform(get("/api/events/{eventId}/credentials/summary", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(120))
                .andExpect(jsonPath("$.credentialed").value(44))
                .andExpect(jsonPath("$.remaining").value(76));

        verify(service).summary(EVENT_ID, USER_ID);
    }

    @Test
    void routesForSearchQrPrintAndOperatorManagementDelegate() throws Exception {
        CredentialResponse credential = new CredentialResponse("CREDENTIALED", 91L, 81L,
                "CHECKED_IN", LocalDateTime.parse("2026-11-05T09:00:00"), true, "Ana Souza", "a***@example.org");
        CredentialTokenResponse token = new CredentialTokenResponse(EVENT_ID, 81L, "opaque-token");
        when(service.search(eq(EVENT_ID), eq(USER_ID), any()))
                .thenReturn(List.of(new CredentialSearchItem(81L, "Ana Souza", "a***@example.org", "CONFIRMED", true, false)));
        when(service.scan(eq(EVENT_ID), eq(USER_ID), any())).thenReturn(credential);
        when(service.reprint(EVENT_ID, 91L, USER_ID, "A")).thenReturn(credential);
        when(service.myToken(EVENT_ID, USER_ID)).thenReturn(token);
        when(service.rotateMyToken(EVENT_ID, USER_ID)).thenReturn(token);
        when(service.rotateParticipantToken(EVENT_ID, 81L, USER_ID)).thenReturn(token);

        mockMvc.perform(post("/api/events/{eventId}/credentials/manual/search", EVENT_ID)
                        .contentType(APPLICATION_JSON).content("{\"searchTerm\":\"Ana\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].emailMasked").value("a***@example.org"));
        mockMvc.perform(post("/api/events/{eventId}/credentials/scan", EVENT_ID)
                        .contentType(APPLICATION_JSON).content("{\"token\":\"opaque\",\"station\":\"A\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result").value("CREDENTIALED"));
        mockMvc.perform(post("/api/events/{eventId}/credentials/{credentialId}/reprint", EVENT_ID, 91L)
                        .contentType(APPLICATION_JSON).content("{\"station\":\"A\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/events/{eventId}/credentials/my-token", EVENT_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("opaque-token"));
        mockMvc.perform(post("/api/events/{eventId}/credentials/my-token/rotate", EVENT_ID))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/events/{eventId}/credentials/enrollments/{enrollmentId}/token/rotate", EVENT_ID, 81L))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/events/{eventId}/credentials/operators/{userId}", EVENT_ID, USER_ID))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/events/{eventId}/credentials/operators/{operatorUserId}", EVENT_ID, USER_ID))
                .andExpect(status().isNoContent());

        verify(service).search(eq(EVENT_ID), eq(USER_ID), any());
        verify(service).scan(eq(EVENT_ID), eq(USER_ID), any());
        verify(service).reprint(EVENT_ID, 91L, USER_ID, "A");
        verify(service).myToken(EVENT_ID, USER_ID);
        verify(service).rotateMyToken(EVENT_ID, USER_ID);
        verify(service).rotateParticipantToken(EVENT_ID, 81L, USER_ID);
        verify(service).addOperator(EVENT_ID, USER_ID, USER_ID);
        verify(service).removeOperator(EVENT_ID, USER_ID, USER_ID);
    }
}
