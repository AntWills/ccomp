package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.notifications.dto.*;
import com.ccomp.br.domain.events.notifications.enums.*;
import com.ccomp.br.domain.events.notifications.persistence.*;
import com.ccomp.br.domain.events.notifications.application.async.EventNotificationPublisher;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventNotificationService {
    private static final int SELECTED_LIMIT = 500;
    private static final int DELIVERY_BATCH_SIZE = 100;
    private final EventCache eventCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventActivityRepository activityRepository;
    private final EventNotificationRepository notificationRepository;
    private final EventNotificationRecipientRepository recipientRepository;
    private final EventNotificationDslRepository notificationDslRepository;
    private final EventNotificationPublisher publisher;
    private final Clock clock;

    public EventNotificationService(EventCache eventCache, EventAccessPolicy eventAccessPolicy,
            EventActivityRepository activityRepository, EventNotificationRepository notificationRepository,
            EventNotificationRecipientRepository recipientRepository,
            EventNotificationDslRepository notificationDslRepository,
            EventNotificationPublisher publisher, Clock clock) {
        this.eventCache = eventCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.activityRepository = activityRepository;
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.notificationDslRepository = notificationDslRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public EventNotification sendToEventSubscribers(Long eventId, CreateEventNotificationRequest request, UUID senderId) {
        EventDTO event = assertCanEdit(eventId, senderId);
        EventNotification notification = saveNotification(eventId, event.title(), null, senderId,
                NotificationAudience.EVENT, request);
        publisher.dispatch(notification.getId());
        return notification;
    }

    @Transactional
    public EventNotification sendToActivitySubscribers(Long eventId, Long activityId,
            CreateEventNotificationRequest request, UUID senderId) {
        EventDTO event = assertCanEdit(eventId, senderId);
        assertActivityBelongsToEvent(eventId, activityId);
        EventNotification notification = saveNotification(eventId, event.title(), activityId, senderId,
                NotificationAudience.ACTIVITY, request);
        publisher.dispatch(notification.getId());
        return notification;
    }

    @Transactional
    public EventNotification sendToSelectedSubscribers(Long eventId, SendSelectedNotificationRequest request,
            UUID senderId) {
        if (request.recipientUserIds().size() > SELECTED_LIMIT)
            throw new DomainException("A seleção pode conter no máximo 500 inscritos.");
        EventDTO event = assertCanEdit(eventId, senderId);
        if (request.activityId() != null) assertActivityBelongsToEvent(eventId, request.activityId());
        List<UUID> uniqueIds = request.recipientUserIds().stream().distinct().toList();
        List<NotificationRecipientCandidate> candidates = notificationDslRepository.findSelectedRecipients(
                eventId, request.activityId(), uniqueIds);
        if (candidates.size() != uniqueIds.size())
            throw new DomainException("Um ou mais usuários selecionados não estão inscritos no escopo informado.");

        EventNotification notification = saveNotification(eventId, event.title(), request.activityId(), senderId,
                NotificationAudience.SELECTED, request.notification());
        List<EventNotificationRecipient> recipients = candidates.stream()
                .map(candidate -> createRecipient(notification.getId(), candidate)).toList();
        recipients = recipientRepository.saveAll(recipients);
        notification.setRecipientCount(recipients.size());
        notification.setDispatchCompletedAt(LocalDateTime.now(clock));
        notification.setStatus(recipients.isEmpty() ? NotificationStatus.COMPLETED : NotificationStatus.PROCESSING);
        notificationRepository.save(notification);
        for (int i = 0; i < recipients.size(); i += DELIVERY_BATCH_SIZE) {
            publisher.deliver(notification.getId(), recipients.subList(i,
                    Math.min(i + DELIVERY_BATCH_SIZE, recipients.size())).stream()
                    .map(EventNotificationRecipient::getId).toList());
        }
        return notification;
    }

    @Transactional
    public void retryFailed(Long eventId, Long notificationId, UUID senderId) {
        assertCanEdit(eventId, senderId);
        EventNotification notification = notificationRepository.findByIdAndEventId(notificationId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada neste evento."));
        List<EventNotificationRecipient> failed = recipientRepository.findAllByNotificationIdAndEmailStatus(
                notificationId, NotificationEmailStatus.FAILED);
        if (failed.isEmpty()) return;
        failed.forEach(recipient -> {
            recipient.setEmailStatus(NotificationEmailStatus.PENDING);
            recipient.setEmailError(null);
        });
        recipientRepository.saveAll(failed);
        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);
        for (int i = 0; i < failed.size(); i += DELIVERY_BATCH_SIZE) {
            publisher.deliver(notificationId, failed.subList(i,
                    Math.min(i + DELIVERY_BATCH_SIZE, failed.size())).stream()
                    .map(EventNotificationRecipient::getId).toList());
        }
    }

    @Transactional
    public void retryDispatch(Long eventId, Long notificationId, UUID senderId) {
        assertCanEdit(eventId, senderId);
        EventNotification notification = notificationRepository.findByIdAndEventId(notificationId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada neste evento."));
        if (notification.getDispatchCompletedAt() != null) return;
        notification.setStatus(NotificationStatus.QUEUED);
        notificationRepository.save(notification);
        publisher.dispatch(notificationId);
    }

    private EventNotification saveNotification(Long eventId, String eventTitle, Long activityId, UUID senderId,
            NotificationAudience audience, CreateEventNotificationRequest request) {
        return notificationRepository.save(EventNotification.builder()
                .eventId(eventId).activityId(activityId).senderUserId(senderId).audience(audience)
                .eventTitle(eventTitle)
                .title(request.title().trim()).message(request.message().trim())
                .status(NotificationStatus.QUEUED).recipientCount(0).build());
    }

    private EventNotificationRecipient createRecipient(Long notificationId, NotificationRecipientCandidate candidate) {
        return EventNotificationRecipient.builder().notificationId(notificationId).userId(candidate.userId())
                .userName(candidate.userName()).emailAddress(candidate.emailAddress()).build();
    }

    private EventDTO assertCanEdit(Long eventId, UUID userId) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!eventAccessPolicy.canEdit(event, userId))
            throw new AccessDeniedException("Você não tem permissão para enviar notificações neste evento.");
        return event;
    }

    private void assertActivityBelongsToEvent(Long eventId, Long activityId) {
        var activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        if (!Objects.equals(activity.getEventId(), eventId))
            throw new ResourceNotFoundException("Atividade não encontrada neste evento.");
    }
}
