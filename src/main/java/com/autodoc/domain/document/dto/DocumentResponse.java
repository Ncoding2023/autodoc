package com.autodoc.domain.document.dto;

import com.autodoc.domain.document.Document;
import com.autodoc.domain.document.DocumentStatus;
import com.autodoc.domain.template.WritingFormat;

public record DocumentResponse(Long id, Long templateId, String title, WritingFormat writingFormat, String documentType,
                               String bodyContent, String importantNotes, String cautions, DocumentStatus status) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getTemplateId(), document.getTitle(), document.getWritingFormat(),
                document.getDocumentType(), document.getBodyContent(), document.getImportantNotes(), document.getCautions(), document.getStatus());
    }
}
