package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "Invalid Token");
    }
}
