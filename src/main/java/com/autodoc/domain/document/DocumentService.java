package com.autodoc.domain.document;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.document.dto.DocumentCreateRequest;
import com.autodoc.domain.document.dto.DocumentListResponse;
import com.autodoc.domain.document.dto.DocumentResponse;
import com.autodoc.domain.document.dto.DocumentUpdateRequest;
import com.autodoc.domain.sheet.SheetColumnRepository;
import com.autodoc.domain.sheet.SheetRowRepository;
import com.autodoc.domain.template.DocumentTemplate;
import com.autodoc.domain.template.DocumentTemplateRepository;
import com.autodoc.domain.template.TemplateScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentFileRepository documentFileRepository;
    private final SheetColumnRepository sheetColumnRepository;
    private final SheetRowRepository sheetRowRepository;
    private final DocumentTemplateRepository templateRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentFileRepository documentFileRepository,
                           SheetColumnRepository sheetColumnRepository, SheetRowRepository sheetRowRepository,
                           DocumentTemplateRepository templateRepository) {
        this.documentRepository = documentRepository;
        this.documentFileRepository = documentFileRepository;
        this.sheetColumnRepository = sheetColumnRepository;
        this.sheetRowRepository = sheetRowRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional
    public DocumentResponse createDocument(Long currentUserId, DocumentCreateRequest request) {
        validateTemplateAccess(request.templateId(), currentUserId);
        Document document = new Document(currentUserId, request.templateId(), request.title(), request.writingFormat(), request.documentType(),
                request.bodyContent(), request.importantNotes(), request.cautions());
        return DocumentResponse.from(documentRepository.save(document));
    }

    public List<DocumentListResponse> getMyDocuments(Long currentUserId) {
        return documentRepository.findByOwnerUserId(currentUserId).stream().map(DocumentListResponse::from).toList();
    }

    public DocumentResponse getDocument(Long documentId, Long currentUserId) {
        return DocumentResponse.from(getOwnedDocument(documentId, currentUserId));
    }

    @Transactional
    public DocumentResponse updateDocument(Long documentId, Long currentUserId, DocumentUpdateRequest request) {
        Document document = getOwnedDocument(documentId, currentUserId);
        document.updateContent(request.title(), request.documentType(), request.bodyContent(), request.importantNotes(), request.cautions());
        return DocumentResponse.from(document);
    }

    @Transactional
    public void deleteDocument(Long documentId, Long currentUserId) {
        Document document = getOwnedDocument(documentId, currentUserId);
        documentFileRepository.deleteByDocumentId(documentId);
        sheetRowRepository.deleteByDocumentId(documentId);
        sheetColumnRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    private Document getOwnedDocument(Long documentId, Long currentUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        if (!currentUserId.equals(document.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return document;
    }

    private void validateTemplateAccess(Long templateId, Long currentUserId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        if (template.getTemplateScope() == TemplateScope.USER && !currentUserId.equals(template.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
