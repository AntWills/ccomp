package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class DomainException extends ApiException {
    public DomainException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "Domain Exception");
    }
}
