package com.ccomp.br.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitExceededException extends ApiException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Muitas tentativas de login. Tente novamente em alguns instantes.",
                HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
