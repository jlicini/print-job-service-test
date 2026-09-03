package com.adobe.printservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record JobRequestDTO(
        @NotBlank String templateId,
        @NotNull Map<String, Object> parameters
) {
}
