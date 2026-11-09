package com.ccomp.br.domain.events.enrollments.listeners;

import com.ccomp.br.domain.events.activities.external.CheckInChannel;
import com.ccomp.br.domain.events.activities.external.CheckInMessageDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.SendMailDTO;
import com.ccomp.br.shared.exceptions.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
@Slf4j
public class CheckInConsumer {
    private final EnrollmentRepository enrollmentRepository;
    private final EventActivityRepository activityRepository;
    private final UserManagement userManagement;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final Clock clock;

    public CheckInConsumer(
            EnrollmentRepository enrollmentRepository,
            EventActivityRepository activityRepository,
            UserManagement userManagement,
            EmailService emailService,
            EmailTemplateService emailTemplateService,
            Clock clock
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.activityRepository = activityRepository;
        this.userManagement = userManagement;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.clock = clock;
    }

    @RabbitListener(queues = CheckInChannel.QUEUE)
    @Transactional
    public void handler(CheckInMessageDTO dto) {
        try {
            Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId())
                    .orElseThrow(() -> new DomainException("O usuário não está inscrito no evento, mas o check-in foi emitido."));

            enrollment.checkIn(LocalDateTime.now(clock));
            EventActivity activity = activityRepository.findById(dto.activityId())
                    .orElseThrow(() -> new DomainException("A atividade do check-in não existe mais."));
            var user = userManagement.findById(enrollment.getUserId())
                    .orElseThrow(() -> new DomainException("O participante do check-in não existe mais."));
            String subject = "Certificado disponível: " + activity.getTitle();
            String htmlContent = emailTemplateService.render(
                    "activity-certificate-available",
                    subject,
                    Map.of(
                            "participantName", user.name(),
                            "activityTitle", activity.getTitle(),
                            "eventTitle", activity.getEvent().getTitle()
                    )
            );
            emailService.send(new SendMailDTO(user.emailAddress(), subject, htmlContent, true));
        } catch (DomainException e) {
            log.error("\nCódigo: {}\nMessage: {}", e.getStatus(), e.getMessage());
        }
    }
}
