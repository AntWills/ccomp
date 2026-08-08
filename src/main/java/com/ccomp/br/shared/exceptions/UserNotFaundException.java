package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFaundException extends ApiException {
    public UserNotFaundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "User Not Found");
    }
}
