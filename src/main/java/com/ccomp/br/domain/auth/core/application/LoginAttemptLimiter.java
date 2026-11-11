package com.ccomp.br.domain.auth.core.application;

import com.ccomp.br.shared.exceptions.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptLimiter {
    private static final String KEY_PREFIX = "ccomp:auth:login-failures:";
    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return count",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final int maxFailures;
    private final long windowSeconds;

    public LoginAttemptLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${security.auth.login.max-failures:10}") int maxFailures,
            @Value("${security.auth.login.window-seconds:60}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
    }

    public void ensureAllowed(String email) {
        String key = keyFor(email);
        String failures = redisTemplate.opsForValue().get(key);
        if (failures == null || Long.parseLong(failures) < maxFailures) {
            return;
        }

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long retryAfterSeconds = ttl == null || ttl < 1 ? windowSeconds : ttl;
        throw new RateLimitExceededException(retryAfterSeconds);
    }

    public void recordFailure(String email) {
        redisTemplate.execute(RECORD_FAILURE_SCRIPT, List.of(keyFor(email)), Long.toString(windowSeconds));
    }

    public void clear(String email) {
        redisTemplate.delete(keyFor(email));
    }

    private String keyFor(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedEmail.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
