package com.adobe.printservice.dto;

import com.adobe.printservice.model.JobStatus;

import java.time.Instant;
import java.util.Map;

public record JobResponseDTO(
        String id,
        String templateId,
        Map<String, Object> parameters,
        JobStatus status,
        int attempts,
        String errorMessage,
        boolean resultAvailable,
        Instant createdAt,
        Instant updatedAt
) {
}
