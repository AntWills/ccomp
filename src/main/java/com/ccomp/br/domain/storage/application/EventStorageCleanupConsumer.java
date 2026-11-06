package com.ccomp.br.domain.storage.application;

import com.ccomp.br.domain.events.core.external.message.EventStorageCleanupChannel;
import com.ccomp.br.domain.events.core.external.message.EventStorageCleanupMessage;
import com.ccomp.br.domain.storage.external.StorageExternal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStorageCleanupConsumer {
    private final StorageExternal storageExternal;

    @RabbitListener(queues = EventStorageCleanupChannel.QUEUE)
    public void deleteEventFiles(EventStorageCleanupMessage message) {
        for (String fileKey : message.fileKeys()) {
            storageExternal.deleteObject(fileKey);
        }
        log.info("Arquivos associados ao evento removidos do storage: {} arquivo(s).", message.fileKeys().size());
    }
}
