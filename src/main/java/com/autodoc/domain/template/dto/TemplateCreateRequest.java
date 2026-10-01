package com.autodoc.domain.template.dto;

import com.autodoc.domain.template.WritingFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull WritingFormat writingFormat,
        @NotBlank @Size(max = 50) String documentType,
        @NotBlank String structureData,
        @NotBlank String templateStyleData
) {
}
