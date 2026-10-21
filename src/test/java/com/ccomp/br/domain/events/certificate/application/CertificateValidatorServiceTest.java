package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.certificate.dto.CertificateValidationResponse;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCodeRepository;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateValidatorServiceTest {
    private static final UUID USER_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");

    @Mock
    private EventCertificateValidationCodeRepository validationCodeRepository;

    @Mock
    private EventCache eventCache;

    @Mock
    private ActivityCache activityCache;

    @Mock
    private EventActivityDslRepository activityDslRepository;

    @Mock
    private UserManagement userManagement;

    private CertificateValidatorService createService() {
        return new CertificateValidatorService(validationCodeRepository, eventCache,
                activityCache, activityDslRepository, userManagement);
    }

    @Test
    void validate_unknownCode_throwsResourceNotFound() {
        when(validationCodeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService().validate("ZZZZ9999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Certificado não encontrado para o código informado.");
    }

    @Test
    void validate_codeIsLowerOrSpaced_isNormalizedBeforeLookup() {
        stubCommon(codeForEvent());
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(eventCache.findById(10L)).thenReturn(Optional.of(eventDto("Semanas de Tecnologia")));
        when(activityDslRepository.sumWorkloadHoursByCheckIn(USER_ID, 10L))
                .thenReturn(BigDecimal.valueOf(6));

        CertificateValidationResponse response = createService().validate(" abc12345 ");

        verify(validationCodeRepository).findByCode("ABC12345");
        assertThat(response.eventTitle()).isEqualTo("Semanas de Tecnologia");
    }

    @Test
    void validate_eventCertificate_sumsCheckedInWorkloadWithUnit() {
        stubCommon(codeForEvent());
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(eventCache.findById(10L)).thenReturn(Optional.of(eventDto("Semanas de Tecnologia")));
        when(activityDslRepository.sumWorkloadHoursByCheckIn(USER_ID, 10L))
                .thenReturn(BigDecimal.valueOf(6));

        CertificateValidationResponse response = createService().validate("ABC12345");

        assertThat(response.code()).isEqualTo("ABC12345");
        assertThat(response.participantId()).isEqualTo(USER_ID);
        assertThat(response.participantName()).isEqualTo("Ana Souza");
        assertThat(response.eventTitle()).isEqualTo("Semanas de Tecnologia");
        assertThat(response.activityTitle()).isNull();
        assertThat(response.workloadHours()).isEqualTo("6 horas");
        assertThat(response.issuedAt()).isEqualTo(LocalDateTime.of(2026, 10, 12, 10, 0));
    }

    @Test
    void validate_activityCertificate_usesActivityWorkload() {
        stubCommon(codeForActivity());
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(eventCache.findById(10L)).thenReturn(Optional.of(eventDto("Semanas de Tecnologia")));
        when(activityCache.findById(77L)).thenReturn(Optional.of(new ActivityDTO(
                77L, 10L, "Workshop de IA", null, null, null, null, null,
                BigDecimal.valueOf(4), null, null)));

        CertificateValidationResponse response = createService().validate("ABC12345");

        assertThat(response.activityTitle()).isEqualTo("Workshop de IA");
        assertThat(response.workloadHours()).isEqualTo("4 horas");
        verify(activityDslRepository, never()).sumWorkloadHoursByCheckIn(any(), any());
    }

    @Test
    void validate_activityCertificate_nullWorkload_returnsZeroHours() {
        stubCommon(codeForActivity());
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(eventCache.findById(10L)).thenReturn(Optional.of(eventDto("Semanas de Tecnologia")));
        when(activityCache.findById(77L)).thenReturn(Optional.of(new ActivityDTO(
                77L, 10L, "Workshop Sem Carga", null, null, null, null, null,
                null, null, null)));

        assertThat(createService().validate("ABC12345").workloadHours()).isEqualTo("0 horas");
    }

    @Test
    void issueForEvent_existingCode_isReusedWithoutSaving() {
        EventCertificateValidationCode existing = codeForEvent();
        when(validationCodeRepository.findByEventIdAndActivityIdIsNullAndUserId(10L, USER_ID))
                .thenReturn(Optional.of(existing));

        assertThat(createService().issueForEvent(10L, USER_ID)).isSameAs(existing);
        verify(validationCodeRepository, never()).save(any());
    }

    @Test
    void issueForActivity_existingCode_isReusedWithoutSaving() {
        EventCertificateValidationCode existing = codeForActivity();
        when(validationCodeRepository.findByActivityIdAndUserId(77L, USER_ID))
                .thenReturn(Optional.of(existing));

        assertThat(createService().issueForActivity(10L, 77L, USER_ID)).isSameAs(existing);
        verify(validationCodeRepository, never()).save(any());
    }

    @Test
    void issueForActivity_noExistingCode_persistsNewCode() {
        when(validationCodeRepository.findByActivityIdAndUserId(77L, USER_ID))
                .thenReturn(Optional.empty());
        when(validationCodeRepository.existsByCode(anyString())).thenReturn(false);
        when(validationCodeRepository.save(any(EventCertificateValidationCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        createService().issueForActivity(10L, 77L, USER_ID);

        ArgumentCaptor<EventCertificateValidationCode> captor =
                ArgumentCaptor.forClass(EventCertificateValidationCode.class);
        verify(validationCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).containsPattern("^[A-Z0-9]{8}$");
        assertThat(captor.getValue().getActivityId()).isEqualTo(77L);
        assertThat(captor.getValue().getEventId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    private void stubCommon(EventCertificateValidationCode code) {
        when(validationCodeRepository.findByCode(code.getCode())).thenReturn(Optional.of(code));
    }

    private static EventCertificateValidationCode codeForEvent() {
        return EventCertificateValidationCode.builder()
                .id(1L)
                .code("ABC12345")
                .eventId(10L)
                .activityId(null)
                .userId(USER_ID)
                .createdAt(LocalDateTime.of(2026, 10, 12, 10, 0))
                .build();
    }

    private static EventCertificateValidationCode codeForActivity() {
        return EventCertificateValidationCode.builder()
                .id(2L)
                .code("ABC12345")
                .eventId(10L)
                .activityId(77L)
                .userId(USER_ID)
                .createdAt(LocalDateTime.of(2026, 10, 12, 10, 0))
                .build();
    }

    private static UserDTO userDto(String name) {
        return new UserDTO(USER_ID, name, new EmailAddress("ana@exemplo.com"),
                null, null, null, null, null);
    }

    private static EventDTO eventDto(String title) {
        return new EventDTO(10L, title, "slug", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
