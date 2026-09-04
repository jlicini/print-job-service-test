package com.adobe.printservice.exception;

public class JobNotFoundException extends RuntimeException {

    private static final String JOB_NOT_FOUND_MESSAGE = "Job does not exist: %s";

    public JobNotFoundException(String id) {
        super(JOB_NOT_FOUND_MESSAGE.formatted(id));
    }
}
