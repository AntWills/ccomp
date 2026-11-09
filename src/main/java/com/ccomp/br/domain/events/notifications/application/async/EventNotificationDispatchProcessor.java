package com.ccomp.br.domain.events.notifications.application.async;

import com.ccomp.br.domain.events.notifications.dto.NotificationRecipientCandidate;
import com.ccomp.br.domain.events.notifications.dto.NotificationRecipientCursor;
import com.ccomp.br.domain.events.notifications.enums.NotificationAudience;
import com.ccomp.br.domain.events.notifications.enums.NotificationEmailStatus;
import com.ccomp.br.domain.events.notifications.enums.NotificationStatus;
import com.ccomp.br.domain.events.notifications.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventNotificationDispatchProcessor {
    public static final int BATCH_SIZE = 100;
    private final EventNotificationRepository notificationRepository;
    private final EventNotificationRecipientRepository recipientRepository;
    private final EventNotificationDslRepository dslRepository;
    private final EventNotificationPublisher publisher;
    private final Clock clock;

    public EventNotificationDispatchProcessor(EventNotificationRepository notificationRepository,
            EventNotificationRecipientRepository recipientRepository, EventNotificationDslRepository dslRepository,
            EventNotificationPublisher publisher, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.dslRepository = dslRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public PageResult processPage(Long notificationId, NotificationRecipientCursor cursor) {
        EventNotification notification = notificationRepository.findById(notificationId).orElseThrow();
        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);

        List<NotificationRecipientCandidate> fetched = notification.getAudience() == NotificationAudience.EVENT
                ? dslRepository.findEventRecipients(notification.getEventId(), cursor, BATCH_SIZE + 1)
                : dslRepository.findActivityRecipients(notification.getEventId(), notification.getActivityId(), cursor,
                        BATCH_SIZE + 1);
        boolean hasNext = fetched.size() > BATCH_SIZE;
        List<NotificationRecipientCandidate> page = hasNext ? fetched.subList(0, BATCH_SIZE) : fetched;
        List<UUID> userIds = page.stream().map(NotificationRecipientCandidate::userId).toList();
        Map<UUID, EventNotificationRecipient> existing = recipientRepository
                .findAllByNotificationIdAndUserIdIn(notificationId, userIds).stream()
                .collect(Collectors.toMap(EventNotificationRecipient::getUserId, Function.identity()));

        List<EventNotificationRecipient> fresh = page.stream()
                .filter(candidate -> !existing.containsKey(candidate.userId()))
                .map(candidate -> EventNotificationRecipient.builder().notificationId(notificationId)
                        .userId(candidate.userId()).userName(candidate.userName())
                        .emailAddress(candidate.emailAddress()).build())
                .toList();
        recipientRepository.saveAll(fresh);
        recipientRepository.flush();
        fresh.forEach(recipient -> existing.put(recipient.getUserId(), recipient));

        List<Long> deliveryIds = page.stream().map(candidate -> existing.get(candidate.userId()))
                .filter(recipient -> recipient.getEmailStatus() == NotificationEmailStatus.PENDING)
                .map(EventNotificationRecipient::getId).filter(Objects::nonNull).toList();
        publisher.deliver(notificationId, deliveryIds);

        NotificationRecipientCursor nextCursor = page.isEmpty() ? null : new NotificationRecipientCursor(
                page.getLast().createdAt(), page.getLast().cursorId());
        return new PageResult(nextCursor, hasNext);
    }

    @Transactional
    public void completeDispatch(Long notificationId) {
        EventNotification notification = notificationRepository.findById(notificationId).orElseThrow();
        notification.setRecipientCount(Math.toIntExact(recipientRepository.countByNotificationId(notificationId)));
        notification.setDispatchCompletedAt(LocalDateTime.now(clock));
        refreshStatus(notification);
        notificationRepository.save(notification);
    }

    @Transactional
    public void refreshDeliveryStatus(Long notificationId) {
        EventNotification notification = notificationRepository.findById(notificationId).orElseThrow();
        refreshStatus(notification);
        notificationRepository.save(notification);
    }

    private void refreshStatus(EventNotification notification) {
        if (notification.getDispatchCompletedAt() == null) return;
        long pending = recipientRepository.countByNotificationIdAndEmailStatus(notification.getId(),
                NotificationEmailStatus.PENDING)
                + recipientRepository.countByNotificationIdAndEmailStatus(notification.getId(),
                NotificationEmailStatus.PROCESSING);
        long failed = recipientRepository.countByNotificationIdAndEmailStatus(notification.getId(),
                NotificationEmailStatus.FAILED);
        if (pending > 0) notification.setStatus(NotificationStatus.PROCESSING);
        else notification.setStatus(failed > 0 ? NotificationStatus.COMPLETED_WITH_ERRORS : NotificationStatus.COMPLETED);
    }

    public record PageResult(NotificationRecipientCursor nextCursor, boolean hasNext) {}
}
