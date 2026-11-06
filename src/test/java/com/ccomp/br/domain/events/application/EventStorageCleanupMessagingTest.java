package com.ccomp.br.domain.events.application;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.core.external.EventStorageCleanupRequested;
import com.ccomp.br.domain.events.core.external.message.EventStorageCleanupChannel;
import com.ccomp.br.domain.events.core.external.message.EventStorageCleanupMessage;
import com.ccomp.br.domain.events.core.external.message.EventStorageCleanupPublisher;
import com.ccomp.br.domain.storage.application.EventStorageCleanupConsumer;
import com.ccomp.br.domain.storage.external.StorageExternal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventStorageCleanupMessagingTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StorageExternal storageExternal;

    @Test
    void publisherSendsCollectedKeysToTheStorageCleanupQueue() {
        EventStorageCleanupPublisher publisher = new EventStorageCleanupPublisher(rabbitTemplate);
        List<String> fileKeys = List.of("events/cover.png", "activities/file.pdf");

        publisher.publish(new EventStorageCleanupRequested(fileKeys));

        ArgumentCaptor<EventStorageCleanupMessage> message = ArgumentCaptor.forClass(EventStorageCleanupMessage.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EXCHANGE_NAME),
                org.mockito.ArgumentMatchers.eq(EventStorageCleanupChannel.ROUTING),
                message.capture());
        assertThat(message.getValue().fileKeys()).containsExactlyElementsOf(fileKeys);
    }

    @Test
    void consumerDeletesEveryKeyFromTheStorageCleanupMessage() {
        EventStorageCleanupConsumer consumer = new EventStorageCleanupConsumer(storageExternal);
        List<String> fileKeys = List.of("events/cover.png", "activities/file.pdf", "guests/image.webp");

        consumer.deleteEventFiles(new EventStorageCleanupMessage(fileKeys));

        InOrder order = inOrder(storageExternal);
        fileKeys.forEach(key -> order.verify(storageExternal).deleteObject(key));
    }
}
