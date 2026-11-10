package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import com.ccomp.br.domain.events.notifications.application.async.EventNotificationDeliveryProcessor;
import com.ccomp.br.domain.events.notifications.email.EventNotificationEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventNotificationDeliveryProcessorTest {
    @Mock EventNotificationRecipientRepository recipientRepository;
    @Mock EventNotificationRecipientDslRepository recipientDslRepository;
    @Mock EventNotificationRepository notificationRepository;
    @Mock EventNotificationEmailService emailService;
    private EventNotificationDeliveryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EventNotificationDeliveryProcessor(recipientRepository, recipientDslRepository,
                notificationRepository, emailService,
                Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void claimsRecipientBeforeSendingAndPersistsDeliveryResult() {
        EventNotificationRecipient recipient = EventNotificationRecipient.builder().id(4L).notificationId(8L)
                .emailAddress("ana@example.org").userName("Ana").build();
        EventNotification notification = EventNotification.builder().id(8L).title("Aviso")
                .eventTitle("DevConf").message("A sala mudou.").build();
        when(recipientDslRepository.claimPending(eq(4L), any())).thenReturn(1);
        when(recipientRepository.findById(4L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.findById(8L)).thenReturn(Optional.of(notification));

        processor.deliver(8L, 4L);

        verify(emailService).send("ana@example.org", "Ana", "DevConf", "Aviso", "A sala mudou.");
        verify(recipientRepository).save(argThat(saved -> saved.getEmailStatus() == NotificationEmailStatus.SENT
                && saved.getEmailSentAt() != null));
    }

    @Test
    void doesNotSendAgainWhenRecipientWasAlreadyClaimed() {
        when(recipientDslRepository.claimPending(eq(4L), any())).thenReturn(0);

        processor.deliver(8L, 4L);

        verifyNoInteractions(emailService, recipientRepository, notificationRepository);
    }

    @Test
    void recordsEmailFailureForMonitoringAndRetry() {
        EventNotificationRecipient recipient = EventNotificationRecipient.builder().id(4L).notificationId(8L)
                .emailAddress("ana@example.org").userName("Ana").build();
        EventNotification notification = EventNotification.builder().id(8L).title("Aviso")
                .eventTitle("DevConf").message("A sala mudou.").build();
        when(recipientDslRepository.claimPending(eq(4L), any())).thenReturn(1);
        when(recipientRepository.findById(4L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.findById(8L)).thenReturn(Optional.of(notification));
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(emailService).send("ana@example.org", "Ana", "DevConf", "Aviso", "A sala mudou.");

        processor.deliver(8L, 4L);

        verify(recipientRepository).save(argThat(saved -> saved.getEmailStatus() == NotificationEmailStatus.FAILED
                && "SMTP indisponível".equals(saved.getEmailError())));
    }
}
