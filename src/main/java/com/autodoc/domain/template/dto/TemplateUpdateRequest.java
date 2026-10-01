package com.autodoc.domain.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String documentType,
        @NotBlank String structureData,
        @NotBlank String templateStyleData
) {
}
