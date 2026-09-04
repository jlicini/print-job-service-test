package com.adobe.printservice.dto.metrics;

import com.adobe.printservice.model.JobStatus;

import java.util.Map;

public record JobMetricsDTO(
        long total,
        Map<JobStatus, Long> byStatus,
        long totalAttempts,
        long retried
) {
}
