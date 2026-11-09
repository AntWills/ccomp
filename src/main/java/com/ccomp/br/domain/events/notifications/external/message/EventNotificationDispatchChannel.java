package com.ccomp.br.domain.events.notifications.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventNotificationDispatchChannel {
    public static final String QUEUE = MessagingChannel.PREFIX + "event-notification-dispatch.queue";
    public static final String ROUTING = "event.notification.dispatch";
    @Bean MessagingChannel eventNotificationDispatch() {
        return new MessagingChannel("event-notification-dispatch", ROUTING);
    }
}
