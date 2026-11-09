package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.shared.cache.CacheNames;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.UUID;

@Component
@Slf4j
public class EventNotificationCacheInvalidator {
    private final CacheManager cacheManager;

    public EventNotificationCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictUnreadCounts(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        afterCommit(() -> {
            try {
                Cache cache = cacheManager.getCache(CacheNames.EVENT_NOTIFICATION_UNREAD_COUNT);
                if (cache != null) userIds.forEach(cache::evict);
            } catch (RuntimeException exception) {
                log.warn("Não foi possível invalidar as contagens de notificações não lidas no cache.", exception);
            }
        });
    }

    public void evictSentHistory() {
        afterCommit(() -> {
            try {
                Cache cache = cacheManager.getCache(CacheNames.EVENT_NOTIFICATION_SENT_HISTORY);
                if (cache != null) cache.clear();
            } catch (RuntimeException exception) {
                log.warn("Não foi possível invalidar o histórico de notificações no cache.", exception);
            }
        });
    }

    private void afterCommit(Runnable invalidation) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidation.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidation.run();
            }
        });
    }
}
