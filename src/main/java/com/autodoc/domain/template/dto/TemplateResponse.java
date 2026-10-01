package com.autodoc.domain.template.dto;

import com.autodoc.domain.template.DocumentTemplate;
import com.autodoc.domain.template.TemplateScope;
import com.autodoc.domain.template.WritingFormat;

public record TemplateResponse(Long id, TemplateScope scope, String name, WritingFormat writingFormat,
                               String documentType, String structureData, String templateStyleData) {
    public static TemplateResponse from(DocumentTemplate template) {
        return new TemplateResponse(template.getId(), template.getTemplateScope(), template.getName(),
                template.getWritingFormat(), template.getDocumentType(), template.getStructureData(), template.getTemplateStyleData());
    }
}
