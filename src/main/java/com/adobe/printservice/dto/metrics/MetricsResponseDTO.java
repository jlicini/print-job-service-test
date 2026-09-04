package com.adobe.printservice.dto.metrics;

public record MetricsResponseDTO(
        JobMetricsDTO jobs,
        TemplateMetricsDTO templates
) {
}
