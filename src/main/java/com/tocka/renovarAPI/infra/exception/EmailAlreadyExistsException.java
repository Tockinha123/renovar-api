package com.tocka.renovarAPI.infra.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email já cadastrado: " + email);
    }
}