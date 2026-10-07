package com.autodoc.domain.document.dto;

import com.autodoc.domain.template.WritingFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentCreateRequest(
        @NotNull Long templateId,
        @NotBlank @Size(max = 255) String title,
        @NotNull WritingFormat writingFormat,
        @NotBlank @Size(max = 50) String documentType,
        String bodyContent,
        String importantNotes,
        String cautions
) {
}
