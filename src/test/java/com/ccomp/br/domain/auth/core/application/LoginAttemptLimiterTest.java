package com.ccomp.br.domain.auth.core.application;

import com.ccomp.br.shared.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginAttemptLimiterTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        limiter = new LoginAttemptLimiter(redisTemplate, 10, 60);
    }

    @Test
    void allowsEmailBelowFailureLimitUsingHashedNormalizedKey() {
        when(valueOperations.get(anyString())).thenReturn("9");

        limiter.ensureAllowed("  Person@Example.com ");

        verify(valueOperations).get("ccomp:auth:login-failures:" + sha256("person@example.com"));
        verify(redisTemplate, never()).getExpire(anyString(), any());
    }

    @Test
    void rejectsEmailAtLimitAndReturnsRemainingWindow() {
        when(valueOperations.get(anyString())).thenReturn("10");
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(42L);

        assertThatThrownBy(() -> limiter.ensureAllowed("person@example.com"))
                .isInstanceOfSatisfying(RateLimitExceededException.class, exception ->
                        assertThat(exception.getRetryAfterSeconds()).isEqualTo(42));
    }

    @Test
    void recordsFailureAtomicallyWithOneMinuteExpiration() {
        limiter.recordFailure("person@example.com");

        verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq("60"));
    }

    @Test
    void clearsTheNormalizedEmailCounterAfterSuccessfulLogin() {
        limiter.clear(" PERSON@Example.com ");

        verify(redisTemplate).delete("ccomp:auth:login-failures:" + sha256("person@example.com"));
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
