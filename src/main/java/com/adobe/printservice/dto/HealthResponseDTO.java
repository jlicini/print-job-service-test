package com.adobe.printservice.dto;

import com.adobe.printservice.model.HealthStatus;

public record HealthResponseDTO(
        HealthStatus status,
        HealthStatus database,
        HealthStatus worker
) {
}
