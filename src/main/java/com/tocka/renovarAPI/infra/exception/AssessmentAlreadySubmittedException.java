package com.tocka.renovarAPI.infra.exception;

public class AssessmentAlreadySubmittedException extends RuntimeException {
    public AssessmentAlreadySubmittedException(String message) {
        super(message);
    }
}
