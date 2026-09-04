package com.adobe.printservice.repository.projection;

import com.adobe.printservice.model.JobStatus;

public record JobStatusCount(
        JobStatus status,
        long total
) {
}
