package com.ccomp.br.domain.events.core.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventStorageCleanupChannel {
    public static final String QUEUE = MessagingChannel.PREFIX + "event-storage-cleanup.queue";
    public static final String ROUTING = "event.storage.cleanup";

    @Bean
    MessagingChannel eventStorageCleanup() {
        return new MessagingChannel("event-storage-cleanup", ROUTING);
    }
}
