package com.ccomp.br.domain.events.certificate.web;

import com.ccomp.br.domain.events.certificate.application.ActivityCertificateBoxServices;
import com.ccomp.br.domain.events.certificate.application.CertificateBoxServices;
import com.ccomp.br.domain.events.certificate.application.CertificateCoverService;
import com.ccomp.br.domain.events.certificate.application.CertificateServices;
import com.ccomp.br.domain.events.certificate.application.CertificateValidatorService;
import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import com.ccomp.br.domain.events.certificate.persistence.EventActivityBoxTemplate;
import com.ccomp.br.domain.events.certificate.persistence.EventBoxTemplate;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.GlobalExceptionHandler;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventCertificateControllerTest {
    private static final UUID PARTICIPANT_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");
    private static final String PREVIEW_URL = "/api/events/50/certificate-preview";
    private static final String SAVE_BOX_URL = "/api/events/50/certificate-box-template";
    private static final String VALIDATE_URL = "/api/events/certificates/validate/ABC12345";
    private static final String ACTIVITY_CERTIFICATE_URL = "/api/events/activities/77/certificate";
    private static final String BOX_TEMPLATE_URL = "/api/events/50/certificate-box-template";
    private static final String ACTIVITY_BOX_TEMPLATE_URL = "/api/events/50/activity-certificate-box-template";
    private static final String COVER_TEMPLATE_URL = "/api/events/50/certificate-cover-template";
    private static final String SAVE_BOX_BODY = """
            {
              "boxes": [
                {"textTemplate": "{participante.nome}", "x": 0.1, "y": 0.1, "width": 0.5}
              ]
            }
            """;

    @Mock
    private CertificateCoverService certificateCoverService;

    @Mock
    private CertificateBoxServices certificateBoxServices;

    @Mock
    private CertificateServices certificateServices;

    @Mock
    private ActivityCertificateBoxServices activityCertificateBoxServices;

    @Mock
    private CertificateValidatorService certificateValidatorService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "9fab7424-adc0-46b6-a552-6024f60a9bd8")
                .build();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var controller = new EventCertificateController(certificateCoverService,
                certificateBoxServices,
                certificateServices,
                activityCertificateBoxServices,
                certificateValidatorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatePreview_eventNotFound_returns404Json() throws Exception {
        when(certificateCoverService.loadCoverTemplate(eq(50L), any()))
                .thenThrow(new ResourceNotFoundException("Evento não encontrado."));

        mockMvc.perform(get(PREVIEW_URL)
                        .accept(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("Evento não encontrado."));
    }

    @Test
    void generatePreview_accessDenied_returns403Json() throws Exception {
        when(certificateCoverService.loadCoverTemplate(eq(50L), any()))
                .thenThrow(new AccessDeniedException("O usuário não tem acesso a este recurso."));

        mockMvc.perform(get(PREVIEW_URL)
                        .accept(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não tem acesso a este recurso."));
    }

    @Test
    void saveBoxTemplate_accessDenied_returns403Json() throws Exception {
        doThrow(new AccessDeniedException("O usuário não tem acesso a este recurso."))
                .when(certificateBoxServices).saveBoxTemplate(eq(50L), any(), any());

        mockMvc.perform(post(SAVE_BOX_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_BOX_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não tem acesso a este recurso."));
    }

    @Test
    void saveBoxTemplate_domainError_returns400Json() throws Exception {
        doThrow(new DomainException("A lista de caixas não pode estar vazia."))
                .when(certificateBoxServices).saveBoxTemplate(eq(50L), any(), any());

        mockMvc.perform(post(SAVE_BOX_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVE_BOX_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("A lista de caixas não pode estar vazia."));
    }

    @Test
    void validateCertificate_validCode_returnsCertificateData() throws Exception {
        when(certificateValidatorService.validate("ABC12345"))
                .thenReturn(new CertificateValidationResponse(
                        "ABC12345",
                        PARTICIPANT_ID,
                        "Ana Souza",
                        "Semanas de Tecnologia",
                        null,
                        null,
                        LocalDateTime.of(2026, 10, 12, 10, 0)));

        mockMvc.perform(get(VALIDATE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ABC12345"))
                .andExpect(jsonPath("$.participantName").value("Ana Souza"))
                .andExpect(jsonPath("$.eventTitle").value("Semanas de Tecnologia"))
                .andExpect(jsonPath("$.activityTitle").doesNotExist())
                .andExpect(jsonPath("$.workloadHours").doesNotExist())
                .andExpect(jsonPath("$.issuedAt").value("2026-10-12T10:00:00"));
    }

    @Test
    void validateCertificate_unknownCode_returns404Json() throws Exception {
        when(certificateValidatorService.validate(anyString()))
                .thenThrow(new ResourceNotFoundException("Certificado não encontrado para o código informado."));

        mockMvc.perform(get(VALIDATE_URL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]")
                        .value("Certificado não encontrado para o código informado."));
    }

    @Test
    void buildCertificateFromActivity_validRequest_returnsPngWithDisposition() throws Exception {
        when(certificateServices.buildCertificateFromActivity(eq(77L), any()))
                .thenReturn(new StorageExternal.StoredFile(
                        new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2, 3}),
                        MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get(ACTIVITY_CERTIFICATE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificado-atividade-77.png\""));
    }

    @Test
    void buildCertificateFromActivity_notSubscribed_returns404Json() throws Exception {
        when(certificateServices.buildCertificateFromActivity(eq(77L), any()))
                .thenThrow(new ResourceNotFoundException("O usuário não é inscrito nesta atividade."));

        mockMvc.perform(get(ACTIVITY_CERTIFICATE_URL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não é inscrito nesta atividade."));
    }

    @Test
    void buildCertificateFromActivity_checkInNotDone_returns400Json() throws Exception {
        when(certificateServices.buildCertificateFromActivity(eq(77L), any()))
                .thenThrow(new DomainException("O inscrito não fez o check-in nesta atividade."));

        mockMvc.perform(get(ACTIVITY_CERTIFICATE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O inscrito não fez o check-in nesta atividade."));
    }

    @Test
    void getBoxTemplate_returnsTemplateInUse() throws Exception {
        when(certificateBoxServices.loadBoxTemplate(eq(50L), any()))
                .thenReturn(EventBoxTemplate.builder()
                        .eventId(50L)
                        .boxes(List.of(new BoxElementDTO(
                                "Certificamos que {participante.nome}", 0.1, 0.1, 0.5, 25f, null, null)))
                        .variables(List.of("{participante.nome}"))
                        .build());

        mockMvc.perform(get(BOX_TEMPLATE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.boxes[0].textTemplate")
                        .value("Certificamos que {participante.nome}"))
                .andExpect(jsonPath("$.variables[0]").value("{participante.nome}"));
    }

    @Test
    void getBoxTemplate_accessDenied_returns403Json() throws Exception {
        when(certificateBoxServices.loadBoxTemplate(eq(50L), any()))
                .thenThrow(new AccessDeniedException("O usuário não tem acesso a este recurso."));

        mockMvc.perform(get(BOX_TEMPLATE_URL))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não tem acesso a este recurso."));
    }

    @Test
    void getActivityBoxTemplate_returnsTemplateInUse() throws Exception {
        when(activityCertificateBoxServices.loadBoxTemplate(eq(50L), any()))
                .thenReturn(EventActivityBoxTemplate.builder()
                        .eventId(50L)
                        .boxes(List.of(new BoxElementDTO(
                                "Atividade de {participante.nome}", 0.1, 0.1, 0.5, 25f, null, null)))
                        .variables(List.of("{participante.nome}"))
                        .build());

        mockMvc.perform(get(ACTIVITY_BOX_TEMPLATE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.boxes[0].textTemplate").value("Atividade de {participante.nome}"))
                .andExpect(jsonPath("$.variables[0]").value("{participante.nome}"));
    }

    @Test
    void getActivityBoxTemplate_accessDenied_returns403Json() throws Exception {
        when(activityCertificateBoxServices.loadBoxTemplate(eq(50L), any()))
                .thenThrow(new AccessDeniedException("O usuário não tem acesso a este recurso."));

        mockMvc.perform(get(ACTIVITY_BOX_TEMPLATE_URL))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não tem acesso a este recurso."));
    }

    @Test
    void getCoverTemplate_returnsImageInUse() throws Exception {
        when(certificateCoverService.loadCoverTemplate(eq(50L), any()))
                .thenReturn(new StorageExternal.StoredFile(
                        new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2, 3}),
                        MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get(COVER_TEMPLATE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }

    @Test
    void getCoverTemplate_accessDenied_returns403Json() throws Exception {
        when(certificateCoverService.loadCoverTemplate(eq(50L), any()))
                .thenThrow(new AccessDeniedException("O usuário não tem acesso a este recurso."));

        mockMvc.perform(get(COVER_TEMPLATE_URL)
                        .accept(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não tem acesso a este recurso."));
    }

    @Test
    void getCoverTemplate_eventNotFound_returns404Json() throws Exception {
        when(certificateCoverService.loadCoverTemplate(eq(50L), any()))
                .thenThrow(new ResourceNotFoundException("Evento não encontrado."));

        mockMvc.perform(get(COVER_TEMPLATE_URL)
                        .accept(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("Evento não encontrado."));
    }

    @Test
    void deleteCoverTemplate_removed_returnsNoContent() throws Exception {
        when(certificateCoverService.deleteCoverTemplate(eq(50L), any())).thenReturn(true);

        mockMvc.perform(delete(COVER_TEMPLATE_URL))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCoverTemplate_withoutSavedCover_returns404Json() throws Exception {
        when(certificateCoverService.deleteCoverTemplate(eq(50L), any())).thenReturn(false);

        mockMvc.perform(delete(COVER_TEMPLATE_URL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.response")
                        .value("Este evento não possui um fundo de certificado definido."));
    }

    @Test
    void deleteCoverTemplate_accessDenied_returns403Json() throws Exception {
        doThrow(new AccessDeniedException("O usuário não tem acesso a este recurso."))
                .when(certificateCoverService).deleteCoverTemplate(eq(50L), any());

        mockMvc.perform(delete(COVER_TEMPLATE_URL))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0]").value("O usuário não tem acesso a este recurso."));
    }
}
