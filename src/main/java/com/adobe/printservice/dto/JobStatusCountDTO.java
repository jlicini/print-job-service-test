package com.adobe.printservice.dto;

import com.adobe.printservice.model.JobStatus;

public record JobStatusCountDTO(
        JobStatus status,
        long total
) {
}
