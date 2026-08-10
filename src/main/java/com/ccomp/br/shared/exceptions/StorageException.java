package com.ccomp.br.shared.exceptions;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {
    public StorageException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "Bad Request");
    }
}
