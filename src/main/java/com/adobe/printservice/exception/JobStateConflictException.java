package com.adobe.printservice.exception;

import com.adobe.printservice.model.JobStatus;

public class JobStateConflictException extends RuntimeException {

    private static final String EXPECTED_STATE_MESSAGE = "Job %s must be in %s state";
    private static final String FAILED_JOB_MESSAGE = "Job %s failed: %s";
    private static final String RESULT_UNAVAILABLE_MESSAGE = "Job result is not available while job %s is in %s state";

    private JobStateConflictException(String message) {
        super(message);
    }

    public static JobStateConflictException requiresStatus(
            String jobId,
            JobStatus expectedStatus
    ) {
        return new JobStateConflictException(EXPECTED_STATE_MESSAGE.formatted(jobId, expectedStatus));
    }

    public static JobStateConflictException cannotFetchResult(
            String jobId,
            JobStatus currentStatus,
            String errorMessage
    ) {
        String message = currentStatus == JobStatus.FAILED
                ? FAILED_JOB_MESSAGE.formatted(jobId, errorMessage)
                : RESULT_UNAVAILABLE_MESSAGE.formatted(jobId, currentStatus);

        return new JobStateConflictException(message);
    }
}
