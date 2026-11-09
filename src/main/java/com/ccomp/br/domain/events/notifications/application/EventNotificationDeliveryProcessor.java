package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EventNotificationDeliveryProcessor {
    private final EventNotificationRecipientRepository recipientRepository;
    private final EventNotificationRepository notificationRepository;
    private final EmailService emailService;
    private final EmailTemplateService templateService;
    private final Clock clock;

    public EventNotificationDeliveryProcessor(EventNotificationRecipientRepository recipientRepository,
            EventNotificationRepository notificationRepository, EmailService emailService,
            EmailTemplateService templateService, Clock clock) {
        this.recipientRepository = recipientRepository;
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.templateService = templateService;
        this.clock = clock;
    }

    @Transactional
    public void deliver(Long notificationId, Long recipientId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (recipientRepository.claimPending(recipientId, NotificationEmailStatus.PENDING,
                NotificationEmailStatus.PROCESSING, now) == 0) return;

        EventNotificationRecipient recipient = recipientRepository.findById(recipientId).orElseThrow();
        EventNotification notification = notificationRepository.findById(notificationId).orElseThrow();
        try {
            String subject = notification.getTitle();
            String html = templateService.render("event-notification", subject, Map.of(
                    "recipientName", recipient.getUserName(),
                    "message", notification.getMessage(),
                    "eventTitle", notification.getEventTitle()
            ));
            emailService.send(new SendMailDTO(new EmailAddress(recipient.getEmailAddress()), subject, html, true));
            recipient.setEmailStatus(NotificationEmailStatus.SENT);
            recipient.setEmailSentAt(LocalDateTime.now(clock));
            recipient.setEmailError(null);
        } catch (RuntimeException exception) {
            recipient.setEmailStatus(NotificationEmailStatus.FAILED);
            String error = exception.getMessage();
            recipient.setEmailError(error == null ? exception.getClass().getSimpleName()
                    : error.substring(0, Math.min(error.length(), 4000)));
        }
        recipientRepository.save(recipient);
    }
}
