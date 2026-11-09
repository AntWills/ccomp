package com.ccomp.br.domain.events.notifications.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventNotificationDeliveryChannel {
    public static final String QUEUE = MessagingChannel.PREFIX + "event-notification-delivery.queue";
    public static final String ROUTING = "event.notification.delivery";
    @Bean MessagingChannel eventNotificationDelivery() {
        return new MessagingChannel("event-notification-delivery", ROUTING);
    }
}
