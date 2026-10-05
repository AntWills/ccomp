package com.ccomp.br.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;

@Slf4j
@Configuration
public class CacheResilienceConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                report("ler", cache, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                // Optional vazio vira null e o RedisCache recusa (disableCachingNullValues).
                // Não é falha de infraestrutura: apenas não há o que cachear.
                if (value == null && e instanceof IllegalArgumentException) {
                    log.debug("Valor nulo não cacheado em '{}' (key={})", cache.getName(), key);
                    return;
                }
                report("gravar", cache, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                report("invalidar", cache, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                report("limpar", cache, e);
            }
        };
    }

    private void report(String operation, Cache cache, RuntimeException e) {
        if (isConnectionFailure(e)) {
            log.error("Redis indisponível ao {} cache '{}', seguindo sem cache", operation, cache.getName(), e);
        } else {
            log.error("Falha ao {} cache '{}' ({})", operation, cache.getName(), e.getClass().getSimpleName(), e);
        }
    }

    private boolean isConnectionFailure(RuntimeException e) {
        return e instanceof RedisConnectionFailureException
                || e instanceof DataAccessResourceFailureException;
    }
}