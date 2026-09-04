package com.adobe.printservice.web;

import com.adobe.printservice.dto.HealthResponseDTO;
import com.adobe.printservice.model.HealthStatus;
import com.adobe.printservice.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthResource {

    private final HealthService healthService;

    public HealthResource(HealthService healthService) {
        this.healthService = healthService;
    }

    @Operation(summary = "Check service liveness")
    @GetMapping("/liveness")
    public Map<String, HealthStatus> liveness() {
        return Map.of("status", HealthStatus.UP);
    }

    @Operation(summary = "Check service readiness")
    @GetMapping("/readiness")
    public ResponseEntity<HealthResponseDTO> readiness() {
        HealthResponseDTO health = healthService.readiness();

        return ResponseEntity
                .status(health.status() == HealthStatus.UP
                        ? HttpStatus.OK
                        : HttpStatus.SERVICE_UNAVAILABLE)
                .body(health);
    }
}
