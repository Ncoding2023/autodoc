package com.autodoc.domain.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import com.autodoc.domain.sheet.dto.SheetCreateRequest;

public record DocumentUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50) String documentType,
        String bodyContent,
        String importantNotes,
        String cautions,
        @Valid SheetCreateRequest sheet
) {
    public DocumentUpdateRequest(String title, String documentType, String bodyContent, String importantNotes, String cautions) {
        this(title, documentType, bodyContent, importantNotes, cautions, null);
    }
}
