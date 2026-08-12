package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class UserBlockedException extends ApiException {
    public UserBlockedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "User Blocked");
    }
}
