package com.ccomp.br.domain.events.enrollments.listeners;

import java.util.Optional;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.external.CheckInMessageDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInConsumerTest {
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EventActivityRepository activityRepository;
    @Mock private UserManagement userManagement;
    @Mock private EmailService emailService;
    @Mock private EmailTemplateService emailTemplateService;

    @Test
    void updatesEventCheckInThenEmailsParticipantThatActivityCertificateIsAvailable() {
        long enrollmentId = 12L;
        long activityId = 34L;
        UUID userId = UUID.randomUUID();
        Enrollment enrollment = Enrollment.builder().id(enrollmentId).userId(userId).build();
        Event event = Event.builder().title("Evento de teste").build();
        EventActivity activity = EventActivity.builder().title("Atividade de teste").event(event).build();
        UserDTO user = new UserDTO(userId, "Pessoa", new EmailAddress("pessoa@example.org"),
                null, null, null, null, null);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(userManagement.findById(userId)).thenReturn(Optional.of(user));
        when(emailTemplateService.render("activity-certificate-available", "Certificado disponível: Atividade de teste",
                java.util.Map.of("participantName", "Pessoa", "activityTitle", "Atividade de teste", "eventTitle", "Evento de teste")))
                .thenReturn("html");

        new CheckInConsumer(enrollmentRepository, activityRepository, userManagement, emailService, emailTemplateService)
                .handler(new CheckInMessageDTO(enrollmentId, activityId));

        verify(emailService).send(org.mockito.ArgumentMatchers.argThat(mail ->
                mail.to().getValue().equals("pessoa@example.org")
                        && mail.subject().equals("Certificado disponível: Atividade de teste")
                        && mail.text().equals("html")));
    }
}
