package com.adobe.printservice.exception;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String id) {
        super("Job does not exist: " + id);
    }
}
