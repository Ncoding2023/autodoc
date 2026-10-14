package com.autodoc.domain.document;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.document.dto.DocumentCreateRequest;
import com.autodoc.domain.document.dto.DocumentListResponse;
import com.autodoc.domain.document.dto.DocumentResponse;
import com.autodoc.domain.document.dto.DocumentUpdateRequest;
import com.autodoc.domain.sheet.SheetColumnRepository;
import com.autodoc.domain.sheet.SheetRowRepository;
import com.autodoc.domain.sheet.SheetService;
import com.autodoc.domain.template.WritingFormat;
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
    private final SheetService sheetService;

    public DocumentService(DocumentRepository documentRepository, DocumentFileRepository documentFileRepository,
                           SheetColumnRepository sheetColumnRepository, SheetRowRepository sheetRowRepository,
                           DocumentTemplateRepository templateRepository, SheetService sheetService) {
        this.documentRepository = documentRepository;
        this.documentFileRepository = documentFileRepository;
        this.sheetColumnRepository = sheetColumnRepository;
        this.sheetRowRepository = sheetRowRepository;
        this.templateRepository = templateRepository;
        this.sheetService = sheetService;
    }

    @Transactional
    public DocumentResponse createDocument(Long currentUserId, DocumentCreateRequest request) {
        DocumentTemplate template = validateTemplateAccess(request.templateId(), currentUserId);
        if (template.getWritingFormat() != request.writingFormat()) {
            throw new BusinessException(ErrorCode.INVALID_DOCUMENT_FORMAT);
        }
        if (request.sheet() != null && request.writingFormat() != WritingFormat.SHEETS)
            throw new BusinessException(ErrorCode.INVALID_DOCUMENT_FORMAT);
        Document document = new Document(currentUserId, request.templateId(), request.title(), request.writingFormat(), request.documentType(),
                request.bodyContent(), request.importantNotes(), request.cautions());
        documentRepository.save(document);
        if (document.getWritingFormat() == WritingFormat.SHEETS) {
            if (request.sheet() == null) sheetService.initializeTemplateColumns(document.getId(), currentUserId, template.getStructureData());
            else sheetService.createSheet(document.getId(), currentUserId, request.sheet());
        }
        return DocumentResponse.from(document);
    }

    public List<DocumentListResponse> getMyDocuments(Long currentUserId) {
        return documentRepository.findByOwnerUserId(currentUserId).stream().map(DocumentListResponse::from).toList();
    }

    public DocumentResponse getDocument(Long documentId, Long currentUserId) {
        return DocumentResponse.from(getOwnedDocument(documentId, currentUserId));
    }

    @Transactional
    public DocumentResponse updateDocument(Long documentId, Long currentUserId, DocumentUpdateRequest request) {
        Document document = getOwnedDocumentForUpdate(documentId, currentUserId);
        if (request.sheet() != null) {
            if (document.getWritingFormat() != WritingFormat.SHEETS)
                throw new BusinessException(ErrorCode.INVALID_DOCUMENT_FORMAT);
            sheetService.replaceSheet(documentId, currentUserId, request.sheet());
        }
        document.updateContent(request.title(), request.documentType(), request.bodyContent(), request.importantNotes(), request.cautions());
        return DocumentResponse.from(document);
    }

    @Transactional
    public void deleteDocument(Long documentId, Long currentUserId) {
        Document document = getOwnedDocumentForUpdate(documentId, currentUserId);
        documentFileRepository.deleteByDocumentId(documentId);
        sheetRowRepository.deleteByDocumentId(documentId);
        sheetColumnRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    private Document getOwnedDocument(Long documentId, Long currentUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        return verifyOwnership(document, currentUserId);
    }

    private Document getOwnedDocumentForUpdate(Long documentId, Long currentUserId) {
        Document document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        return verifyOwnership(document, currentUserId);
    }

    private Document verifyOwnership(Document document, Long currentUserId) {
        if (!currentUserId.equals(document.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return document;
    }

    private DocumentTemplate validateTemplateAccess(Long templateId, Long currentUserId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
        if (template.getTemplateScope() == TemplateScope.USER && !currentUserId.equals(template.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return template;
    }
}
