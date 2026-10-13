package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateVariablesServicesTest {
    private static final UUID USER_ID = UUID.fromString("9fab7424-adc0-46b6-a552-6024f60a9bd8");
    private static final String VALIDATION_BASE_URL = "https://exemplo.com/validar/";

    @Mock
    private EnrollmentActivityDslRepository enrollmentActivityDslRepository;

    @Mock
    private EventActivityDslRepository activityDslRepository;

    @Mock
    private UserManagement userManagement;

    @Mock
    private EventCache eventCache;

    @Mock
    private CertificateValidatorService validatorService;

    @Mock
    private EventActivityRepository activityRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EventRepository eventRepository;

    private CertificateVariablesServices createService() {
        CertificateVariablesServices service = new CertificateVariablesServices(
                enrollmentActivityDslRepository,
                activityDslRepository,
                userManagement,
                eventCache,
                validatorService,
                activityRepository,
                enrollmentRepository,
                eventRepository);
        ReflectionTestUtils.setField(service, "validationBaseUrl", VALIDATION_BASE_URL);
        return service;
    }

    @Test
    void loadEventVariables_nullUser_throwsAccessDenied() {
        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of("{participante.nome}"), 10L, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("É necessário estar autenticado para gerar o certificado.");
    }

    @Test
    void loadEventVariables_eventNotFound_throws() {
        when(eventRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento não encontrado.");
    }

    @Test
    void loadEventVariables_notEnrolled_throws() {
        Event event = stubEvent();
        when(enrollmentRepository.findByUserIdAndEvent(USER_ID, event))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("O usuário não é inscrito no evento.");
    }

    @Test
    void loadEventVariables_checkInNotDone_throwsDomain() {
        Event event = stubEvent();
        when(enrollmentRepository.findByUserIdAndEvent(USER_ID, event))
                .thenReturn(Optional.of(Enrollment.builder().userId(USER_ID).event(event).build()));

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("O credenciamento (check-in) no evento não foi realizado.");
    }

    @Test
    void loadEventVariables_withCheckIn_returnsRealValuesWithCode() {
        Event event = stubEvent();

        Enrollment enrollment = Enrollment.builder().userId(USER_ID).event(event).build();
        enrollment.checkIn();
        when(enrollmentRepository.findByUserIdAndEvent(USER_ID, event))
                .thenReturn(Optional.of(enrollment));

        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(activityDslRepository.sumWorkloadHoursByCheckIn(USER_ID, 10L))
                .thenReturn(BigDecimal.valueOf(6));
        when(validatorService.issueForEvent(10L, USER_ID))
                .thenReturn(validationCode());

        Map<String, String> values = createService().loadEventVariablesValues(List.of(
                "{participante.nome}",
                "{evento.title}",
                "{evento.local}",
                "{evento.carga_horaria}",
                "{validacao.codigo}",
                "{validacao.url}"
        ), 10L, USER_ID);

        assertThat(values)
                .containsEntry("{participante.nome}", "Ana Souza")
                .containsEntry("{evento.title}", "Semanas de Tecnologia")
                .containsEntry("{evento.local}", "Auditório Central")
                .containsEntry("{evento.carga_horaria}", "6")
                .containsEntry("{validacao.codigo}", "ABC12345")
                .containsEntry("{validacao.url}", "https://exemplo.com/validar/ABC12345");
    }

    @Test
    void loadActivityVariables_buildsValidationUrlWithCode() {
        EventActivity activity = EventActivity.builder()
                .title("Workshop de IA")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(1L)
                .workloadHours(BigDecimal.valueOf(4))
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .build();
        // Entidade gerenciada pelo JPA teria id/eventId populados na carga
        activity.setId(77L);
        activity.setEvent(event());
        activity.setEventId(10L);

        Enrollment enrollment = Enrollment.builder()
                .userId(USER_ID)
                .event(event())
                .status(EnumEnrollmentState.CHECKED_IN)
                .build();

        EnrollmentActivity enrollmentActivity = new EnrollmentActivity(enrollment, activity);
        enrollmentActivity.makeAttendance();

        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(USER_ID, 77L))
                .thenReturn(Optional.of(enrollmentActivity));
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(activityRepository.findById(77L)).thenReturn(Optional.of(activity));
        when(validatorService.issueForActivity(10L, 77L, USER_ID))
                .thenReturn(validationCode());

        Map<String, String> values = createService().loadActivityVariablesValues(List.of(
                "{participante.nome}",
                "{atividade.nome}",
                "{atividade.carga_horaria}",
                "{validacao.codigo}",
                "{validacao.url}"
        ), 77L, USER_ID);

        assertThat(values)
                .containsEntry("{participante.nome}", "Ana Souza")
                .containsEntry("{atividade.nome}", "Workshop de IA")
                .containsEntry("{atividade.carga_horaria}", "4 horas")
                .containsEntry("{validacao.codigo}", "ABC12345")
                .containsEntry("{validacao.url}", "https://exemplo.com/validar/ABC12345");
    }

    private Event stubEvent() {
        Event event = event();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        return event;
    }

    private static Event event() {
        return Event.builder()
                .title("Semanas de Tecnologia")
                .slug("semanas-tecnologia")
                .address("Auditório Central")
                .build();
    }

    private static EventCertificateValidationCode validationCode() {
        return EventCertificateValidationCode.builder()
                .id(3L)
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
}
