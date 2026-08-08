package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class BadCredentialsException extends ApiException {
    public BadCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "Bad Credentials");
    }
}
