package com.ccomp.br.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public abstract class ApiException extends RuntimeException {
    @Getter
    private final HttpStatus status;
    private final String title;

    protected ApiException(String message, HttpStatus status, String title) {
        super(message);
        this.status = status;
        this.title = title;
    }

    public ErrorResponse toErrorResponse(String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                title,
                path,
                List.of(getMessage()),
                null
        );
    }
}
