package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.enums.EnumActivityType;
import com.ccomp.br.domain.events.activities.enums.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.certificate.persistence.EventCertificateValidationCode;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.enums.EnumEventExecutionStatus;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.enrollments.enums.EnumEnrollmentState;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
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
import static org.mockito.Mockito.mock;

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
    private ActivityCache activityCache;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private CertificateVariablesServices createService() {
        CertificateVariablesServices service = new CertificateVariablesServices(
                enrollmentActivityDslRepository,
                activityDslRepository,
                userManagement,
                eventCache,
                validatorService,
                activityCache,
                enrollmentRepository);
        ReflectionTestUtils.setField(service, "validationBaseUrl", VALIDATION_BASE_URL);
        return service;
    }

    @Test
    void loadEventVariables_eventNotFound_throws() {
        when(eventCache.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento não encontrado.");
    }

    @Test
    void loadEventVariables_eventNotFinished_throwsBeforeCheckingEnrollment() {
        Event event = Event.builder()
                .title("Evento em andamento")
                .slug("evento-em-andamento")
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(1))
                .build();
        when(eventCache.findById(10L)).thenReturn(Optional.of(toDto(event)));

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("O certificado do evento só pode ser emitido após seu encerramento.");
    }

    @Test
    void loadEventVariables_addsBaseWorkloadToAttendedActivityWorkload() {
        Event event = finishedEvent(null);
        event.setWorkloadHours(new BigDecimal("2.5"));
        Enrollment enrollment = mock(Enrollment.class);
        when(eventCache.findById(10L)).thenReturn(Optional.of(toDto(event)));
        when(enrollmentRepository.findByUserIdAndEventId(USER_ID, 10L)).thenReturn(Optional.of(enrollment));
        when(enrollment.haveCheckIn()).thenReturn(true);
        when(activityDslRepository.sumWorkloadHoursByCheckIn(USER_ID, 10L)).thenReturn(new BigDecimal("3.5"));

        Map<String, String> values = createService().loadEventVariablesValues(
                List.of("{evento.carga_horaria}"), 10L, USER_ID);

        assertThat(values.get("{evento.carga_horaria}")).isEqualTo("6");
    }

    @Test
    void loadEditorVariables_usesPerEventTeamWorkloadAndDoesNotIssueCodeUnlessRequested() {
        when(eventCache.findById(10L)).thenReturn(Optional.of(finishedEventDto(new BigDecimal("8.5"))));
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(user("Editor")));

        Map<String, String> values = createService().loadEditorVariablesValues(
                List.of("{evento.carga_horaria}"), 10L, USER_ID);

        assertThat(values.get("{evento.carga_horaria}")).isEqualTo("8.5");
        assertThat(values.get("{evento.organizacao}")).isEqualTo("Organização de teste");
        assertThat(values.get("{equipe.carga_horaria}")).isEqualTo("8.5");
        assertThat(values.get("{equipe.nome}")).isEqualTo("Editor");
    }

    @Test
    void loadInstructorVariables_usesDslWorkloadFromConfirmedAssignments() {
        Event event = finishedEvent();
        when(eventCache.findById(10L)).thenReturn(Optional.of(toDto(event)));
        when(activityDslRepository.findConfirmedInstructorAccount(12L, 10L)).thenReturn(Optional.of(USER_ID));
        when(activityDslRepository.sumWorkloadHoursByInstructor(USER_ID, 10L)).thenReturn(new BigDecimal("3.5"));
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(user("Ministrante")));

        Map<String, String> values = createService().loadInstructorVariablesValues(
                List.of("{evento.carga_horaria}"), 10L, 12L, USER_ID);

        assertThat(values.get("{evento.carga_horaria}")).isEqualTo("3.5");
        assertThat(values.get("{ministrante.carga_horaria}")).isEqualTo("3.5");
        assertThat(values.get("{ministrante.nome}")).isEqualTo("Ministrante");
    }

    private Event finishedEvent() {
        return finishedEvent(null);
    }

    private Event finishedEvent(BigDecimal teamWorkloadHours) {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 8, 12, 0);
        return Event.builder()
                .id(10L)
                .title("Evento")
                .startDate(referenceTime.minusDays(2))
                .endDate(referenceTime.minusDays(1))
                .organization("Organização de teste")
                .teamCertificateWorkloadHours(teamWorkloadHours)
                .ownerId(UUID.randomUUID())
                .build();
    }

    private EventDTO finishedEventDto(BigDecimal teamWorkloadHours) {
        return new EventDTO(10L, "Evento", "evento", null, null, null,
                null, null, null, EnumEventStatus.PUBLISHED,
                null, null, EnumEventExecutionStatus.FINISHED,
                null, null, null, null, USER_ID, null, null, "Auditório",
                null, teamWorkloadHours, "Organização de teste");
    }

    private UserDTO user(String name) {
        return new UserDTO(USER_ID, name, new EmailAddress("user@example.com"),
                null, null, null, null, null);
    }

    @Test
    void loadEventVariables_notEnrolled_throws() {
        Event event = stubEvent();
        when(enrollmentRepository.findByUserIdAndEventId(USER_ID, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("O usuário não é inscrito no evento.");
    }

    @Test
    void loadEventVariables_checkInNotDone_throwsDomain() {
        Event event = stubEvent();
        when(enrollmentRepository.findByUserIdAndEventId(USER_ID, 10L))
                .thenReturn(Optional.of(Enrollment.builder().userId(USER_ID).event(event).build()));

        assertThatThrownBy(() -> createService()
                .loadEventVariablesValues(List.of(), 10L, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("O participante não fez check-in no evento.");
    }

    @Test
    void loadEventVariables_withCheckIn_returnsRealValuesWithCode() {
        Event event = stubEvent();

        Enrollment enrollment = Enrollment.builder().userId(USER_ID).event(event).build();
        enrollment.checkIn(LocalDateTime.now());
        when(enrollmentRepository.findByUserIdAndEventId(USER_ID, 10L))
                .thenReturn(Optional.of(enrollment));

        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(activityDslRepository.sumWorkloadHoursByCheckIn(USER_ID, 10L))
                .thenReturn(BigDecimal.valueOf(6));
        when(validatorService.issueForEvent(10L, USER_ID))
                .thenReturn(validationCode());

        Map<String, String> values = createService().loadEventVariablesValues(List.of(
                "{participante.nome}",
                "{evento.title}",
                "{evento.data_inicio}",
                "{evento.data_fim}",
                "{evento.local}",
                "{evento.carga_horaria}",
                "{validacao.codigo}",
                "{validacao.url}"
        ), 10L, USER_ID);

        assertThat(values)
                .containsEntry("{participante.nome}", "Ana Souza")
                .containsEntry("{evento.title}", "Semanas de Tecnologia")
                .containsEntry("{evento.data_inicio}", "06/11/2026")
                .containsEntry("{evento.data_fim}", "07/11/2026")
                .containsEntry("{evento.local}", "Auditório Central")
                .containsEntry("{evento.carga_horaria}", "6")
                .containsEntry("{validacao.codigo}", "ABC12345")
                .containsEntry("{validacao.url}", "https://exemplo.com/validar/ABC12345");
    }

    @Test
    void loadActivityVariables_returnsEventStartAndEndDates() {
        Event event = finishedEvent();
        EventActivity activity = EventActivity.builder()
                .title("Workshop")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .event(event)
                .build();
        activity.setId(77L);
        activity.setEventId(10L);
        Enrollment enrollment = Enrollment.builder().userId(USER_ID).event(event)
                .status(EnumEnrollmentState.CHECKED_IN).build();
        EnrollmentActivity enrollmentActivity = new EnrollmentActivity(enrollment, activity);
        enrollmentActivity.makeAttendance(LocalDateTime.now());

        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(USER_ID, 77L))
                .thenReturn(Optional.of(enrollmentActivity));
        when(activityCache.findById(77L)).thenReturn(Optional.of(new ActivityDTO(
                77L, 10L, "Workshop", null, null, "Sala", null, null, null, null, null)));
        when(eventCache.findById(10L)).thenReturn(Optional.of(toDto(event)));

        Map<String, String> values = createService().loadActivityVariablesValues(List.of(
                "{evento.data_inicio}", "{evento.data_fim}"), 77L, USER_ID);

        assertThat(values)
                .containsEntry("{evento.data_inicio}", "06/11/2026")
                .containsEntry("{evento.data_fim}", "07/11/2026");
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
        enrollmentActivity.makeAttendance(LocalDateTime.now());

        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(USER_ID, 77L))
                .thenReturn(Optional.of(enrollmentActivity));
        when(userManagement.findById(USER_ID)).thenReturn(Optional.of(userDto("Ana Souza")));
        when(activityCache.findById(77L)).thenReturn(Optional.of(new ActivityDTO(
                77L, 10L, "Workshop de IA", null, null, "Sala 1", null, null,
                BigDecimal.valueOf(4), null, null)));
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
                .containsEntry("{atividade.carga_horaria}", "4")
                .containsEntry("{validacao.codigo}", "ABC12345")
                .containsEntry("{validacao.url}", "https://exemplo.com/validar/ABC12345");
    }

    @Test
    void loadActivityVariables_requiresEventCheckInEvenWhenActivityWasAttended() {
        EventActivity activity = EventActivity.builder()
                .title("Workshop de IA")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .build();
        activity.setId(77L);
        activity.setEvent(event());
        activity.setEventId(10L);

        Enrollment enrollment = Enrollment.builder()
                .userId(USER_ID)
                .event(event())
                .status(EnumEnrollmentState.CONFIRMED)
                .build();
        EnrollmentActivity enrollmentActivity = new EnrollmentActivity(enrollment, activity);
        enrollmentActivity.makeAttendance(LocalDateTime.now());
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(USER_ID, 77L))
                .thenReturn(Optional.of(enrollmentActivity));

        assertThatThrownBy(() -> createService().loadActivityVariablesValues(List.of(), 77L, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("O participante não fez check-in no evento.");
    }

    @Test
    void loadActivityVariablesRequiresParticipationInActivityAfterEventCheckIn() {
        EventActivity activity = EventActivity.builder()
                .title("Workshop de IA")
                .type(EnumActivityType.WORKSHOP)
                .displayOrder(1L)
                .registrationPolicy(EnumActivityRegistrationPolicy.PUBLIC)
                .build();
        activity.setId(77L);
        activity.setEvent(event());
        activity.setEventId(10L);

        Enrollment enrollment = Enrollment.builder()
                .userId(USER_ID)
                .event(event())
                .status(EnumEnrollmentState.CHECKED_IN)
                .build();
        EnrollmentActivity enrollmentActivity = new EnrollmentActivity(enrollment, activity);
        when(enrollmentActivityDslRepository.findByUserIdAndActivityId(USER_ID, 77L))
                .thenReturn(Optional.of(enrollmentActivity));

        assertThatThrownBy(() -> createService().loadActivityVariablesValues(List.of(), 77L, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("O inscrito não participou desta atividade.");
    }

    private Event stubEvent() {
        Event event = event();
        when(eventCache.findById(10L)).thenReturn(Optional.of(toDto(event)));
        return event;
    }

    private static EventDTO toDto(Event event) {
        return new EventDTO(10L, event.getTitle(), event.getSlug(), null, null, null,
                null, null, null, event.getStatus(), event.getStartDate(), event.getEndDate(),
                event.getExecutionStatus(LocalDateTime.of(2026, 11, 8, 12, 0)), null, null, null, null, USER_ID, null, null,
                event.getAddress(), event.getWorkloadHours(), event.getTeamCertificateWorkloadHours(),
                event.getOrganization());
    }

    private static Event event() {
        return Event.builder()
                .title("Semanas de Tecnologia")
                .slug("semanas-tecnologia")
                .address("Auditório Central")
                .startDate(LocalDateTime.of(2026, 11, 6, 9, 0))
                .endDate(LocalDateTime.of(2026, 11, 7, 9, 0))
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
