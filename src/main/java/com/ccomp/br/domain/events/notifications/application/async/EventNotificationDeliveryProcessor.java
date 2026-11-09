package com.ccomp.br.domain.events.notifications.application.async;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import com.ccomp.br.domain.events.notifications.email.EventNotificationEmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EventNotificationDeliveryProcessor {
    private final EventNotificationRecipientRepository recipientRepository;
    private final EventNotificationRecipientDslRepository recipientDslRepository;
    private final EventNotificationRepository notificationRepository;
    private final EventNotificationEmailService notificationEmailService;
    private final Clock clock;

    public EventNotificationDeliveryProcessor(EventNotificationRecipientRepository recipientRepository,
            EventNotificationRecipientDslRepository recipientDslRepository,
            EventNotificationRepository notificationRepository, EventNotificationEmailService notificationEmailService,
            Clock clock) {
        this.recipientRepository = recipientRepository;
        this.recipientDslRepository = recipientDslRepository;
        this.notificationRepository = notificationRepository;
        this.notificationEmailService = notificationEmailService;
        this.clock = clock;
    }

    @Transactional
    public void deliver(Long notificationId, Long recipientId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (recipientDslRepository.claimPending(recipientId, now) == 0) {
            return;
        }


        EventNotificationRecipient recipient = recipientRepository.findById(recipientId).orElseThrow();
        EventNotification notification = notificationRepository.findById(notificationId).orElseThrow();
        try {
            notificationEmailService.send(
                    recipient.getEmailAddress(),
                    recipient.getUserName(),
                    notification.getEventTitle(),
                    notification.getTitle(),
                    notification.getMessage()
            );
            recipient.setEmailStatus(NotificationEmailStatus.SENT);
            recipient.setEmailSentAt(LocalDateTime.now(clock));
            recipient.setEmailError(null);
        } catch (RuntimeException exception) {
            recipient.setEmailStatus(NotificationEmailStatus.FAILED);
            String error = exception.getMessage();
            recipient.setEmailError(
                    error == null
                            ? exception.getClass().getSimpleName()
                            : error.substring(0, Math.min(error.length(), 4000))
            );
        }
        recipientRepository.save(recipient);
    }
}
