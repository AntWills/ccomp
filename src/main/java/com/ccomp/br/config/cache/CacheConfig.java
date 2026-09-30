package com.ccomp.br.config.cache;

import com.ccomp.br.shared.cache.CacheNames;
import com.ccomp.br.shared.cache.CacheProperties;
import com.ccomp.br.shared.cache.CacheTier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final String APP_PREFIX = "ccomp";
    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(validator)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .computePrefixWith(cacheName -> {
                    String domain = cacheName.contains(":") ? cacheName.substring(0, cacheName.indexOf(':')) : cacheName;
                    return APP_PREFIX + ":" + domain + ":" + cacheName + "::";
                });
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory,
                                          RedisCacheConfiguration redisCacheConfiguration) {
        Map<String, RedisCacheConfiguration> porCache = CacheNames.TIERS.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> redisCacheConfiguration.entryTtl(resolverTtl(entry.getKey(), entry.getValue()))
                ));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(redisCacheConfiguration.entryTtl(cacheProperties.getDefaultTtl()))
                .withInitialCacheConfigurations(porCache)
                .build();
    }


    private Duration resolverTtl(String cacheName, CacheTier tierStandard) {
        String overrideTierName = cacheProperties.getTierOverrides().get(cacheName);

        CacheTier tierEffective = overrideTierName != null
                ? CacheTier.valueOf(overrideTierName.toUpperCase())
                : tierStandard;

        return cacheProperties.getTiers().getOrDefault(
                tierEffective.name().toLowerCase(),
                cacheProperties.getDefaultTtl()
        );
    }
}