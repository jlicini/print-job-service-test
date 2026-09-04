package com.adobe.printservice.dto.metrics;

public record WorkerMetricsDTO(
        int maxThreads,
        int poolSize,
        int activeThreads,
        int queuedTasks
) {
}
