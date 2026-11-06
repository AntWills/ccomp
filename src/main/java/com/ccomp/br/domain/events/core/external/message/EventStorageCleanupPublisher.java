package com.ccomp.br.domain.events.core.external.message;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.core.external.EventStorageCleanupRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStorageCleanupPublisher {
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EventStorageCleanupRequested request) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    EventStorageCleanupChannel.ROUTING,
                    new EventStorageCleanupMessage(request.fileKeys()));
        } catch (RuntimeException exception) {
            log.error("Não foi possível enfileirar a limpeza dos arquivos do evento no storage.", exception);
        }
    }
}
