package com.adobe.printservice.exception;

public class JobFailedException extends RuntimeException {
    public JobFailedException(String message) {
        super(message);
    }
}
