package com.ccomp.br.domain.events.notifications.listeners;

import com.ccomp.br.domain.events.notifications.application.EventNotificationDispatchProcessor;
import com.ccomp.br.domain.events.notifications.dto.NotificationRecipientCursor;
import com.ccomp.br.domain.events.notifications.external.message.EventNotificationDispatchChannel;
import com.ccomp.br.domain.events.notifications.external.message.EventNotificationDispatchMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventNotificationDispatchConsumer {
    private final EventNotificationDispatchProcessor processor;

    public EventNotificationDispatchConsumer(EventNotificationDispatchProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = EventNotificationDispatchChannel.QUEUE)
    public void dispatch(EventNotificationDispatchMessage message) {
        NotificationRecipientCursor cursor = null;
        EventNotificationDispatchProcessor.PageResult result;
        do {
            result = processor.processPage(message.notificationId(), cursor);
            cursor = result.nextCursor();
        } while (result.hasNext());
        processor.completeDispatch(message.notificationId());
        log.info("Distribuição da notificação {} concluída", message.notificationId());
    }
}
