package com.autodoc.domain.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50) String documentType,
        String bodyContent,
        String importantNotes,
        String cautions
) {
}
