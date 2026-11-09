package com.ccomp.br.domain.events.notifications.listeners;

import com.ccomp.br.domain.events.notifications.application.EventNotificationDeliveryProcessor;
import com.ccomp.br.domain.events.notifications.application.EventNotificationDispatchProcessor;
import com.ccomp.br.domain.events.notifications.external.message.EventNotificationDeliveryBatchMessage;
import com.ccomp.br.domain.events.notifications.external.message.EventNotificationDeliveryChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventNotificationDeliveryConsumer {
    private final EventNotificationDeliveryProcessor deliveryProcessor;
    private final EventNotificationDispatchProcessor dispatchProcessor;

    public EventNotificationDeliveryConsumer(EventNotificationDeliveryProcessor deliveryProcessor,
            EventNotificationDispatchProcessor dispatchProcessor) {
        this.deliveryProcessor = deliveryProcessor;
        this.dispatchProcessor = dispatchProcessor;
    }

    @RabbitListener(queues = EventNotificationDeliveryChannel.QUEUE)
    public void deliver(EventNotificationDeliveryBatchMessage message) {
        for (Long recipientId : message.recipientIds()) {
            deliveryProcessor.deliver(message.notificationId(), recipientId);
        }
        dispatchProcessor.refreshDeliveryStatus(message.notificationId());
        log.info("Lote de notificações {} processado ({} destinatários)", message.notificationId(),
                message.recipientIds().size());
    }
}
