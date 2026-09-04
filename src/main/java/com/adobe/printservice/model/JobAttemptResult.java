package com.adobe.printservice.model;

public enum JobAttemptResult {
    SUCCESS("Render completed successfully"),
    TRANSIENT_FAILURE("Simulated transient rendering failure"),
    INTERRUPTED("Rendering was interrupted");

    private final String message;

    JobAttemptResult(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
