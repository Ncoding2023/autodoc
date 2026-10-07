package com.autodoc.domain.document.dto;

import com.autodoc.domain.document.Document;
import com.autodoc.domain.document.DocumentStatus;
import com.autodoc.domain.template.WritingFormat;

public record DocumentListResponse(Long id, String title, WritingFormat writingFormat, String documentType, DocumentStatus status) {
    public static DocumentListResponse from(Document document) {
        return new DocumentListResponse(document.getId(), document.getTitle(), document.getWritingFormat(), document.getDocumentType(), document.getStatus());
    }
}
