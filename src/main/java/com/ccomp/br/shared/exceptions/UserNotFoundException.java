package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "User Not Found");
    }
}
