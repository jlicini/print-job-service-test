package com.adobe.printservice.web;

import com.adobe.printservice.dto.metrics.MetricsResponseDTO;
import com.adobe.printservice.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsResource {

    private final MetricsService metricsService;

    public MetricsResource(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Operation(summary = "Get application metrics")
    @GetMapping("/metrics")
    public MetricsResponseDTO metrics() {
        return metricsService.metrics();
    }
}
