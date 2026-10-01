package com.autodoc.domain.template;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.template.dto.TemplateCreateRequest;
import com.autodoc.domain.template.dto.TemplateResponse;
import com.autodoc.domain.template.dto.TemplateUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DocumentTemplateService {
    private final DocumentTemplateRepository templateRepository;

    public DocumentTemplateService(DocumentTemplateRepository templateRepository) { this.templateRepository = templateRepository; }

    public List<TemplateResponse> getAvailableTemplates(Long currentUserId) {
        return templateRepository.findByTemplateScopeOrOwnerUserId(TemplateScope.BASIC, currentUserId)
                .stream().map(TemplateResponse::from).toList();
    }

    public TemplateResponse getTemplate(Long templateId, Long currentUserId) {
        return TemplateResponse.from(getAccessibleTemplate(templateId, currentUserId));
    }

    @Transactional
    public TemplateResponse createUserTemplate(Long currentUserId, TemplateCreateRequest request) {
        DocumentTemplate template = new DocumentTemplate(currentUserId, request.name(), request.writingFormat(), request.documentType(),
                request.structureData(), request.templateStyleData());
        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public TemplateResponse updateUserTemplate(Long templateId, Long currentUserId, TemplateUpdateRequest request) {
        DocumentTemplate template = getOwnedUserTemplate(templateId, currentUserId);
        template.update(request.name(), request.documentType(), request.structureData(), request.templateStyleData());
        return TemplateResponse.from(template);
    }

    @Transactional
    public void deleteUserTemplate(Long templateId, Long currentUserId) {
        templateRepository.delete(getOwnedUserTemplate(templateId, currentUserId));
    }

    private DocumentTemplate getAccessibleTemplate(Long templateId, Long currentUserId) {
        DocumentTemplate template = findTemplate(templateId);
        if (template.getTemplateScope() == TemplateScope.USER && !currentUserId.equals(template.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return template;
    }

    private DocumentTemplate getOwnedUserTemplate(Long templateId, Long currentUserId) {
        DocumentTemplate template = findTemplate(templateId);
        if (template.getTemplateScope() != TemplateScope.USER || !currentUserId.equals(template.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return template;
    }

    private DocumentTemplate findTemplate(Long templateId) {
        return templateRepository.findById(templateId).orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
    }
}
