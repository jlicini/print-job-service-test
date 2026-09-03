package com.adobe.printservice.exception;

public class JobResultNotAvailableException extends RuntimeException {
    public JobResultNotAvailableException() {
        super("Job result is not available yet");
    }
}