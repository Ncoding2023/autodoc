package com.autodoc.domain.document;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.document.dto.DocumentCreateRequest;
import com.autodoc.domain.document.dto.DocumentResponse;
import com.autodoc.domain.document.dto.DocumentUpdateRequest;
import com.autodoc.domain.sheet.SheetColumn;
import com.autodoc.domain.sheet.SheetColumnRepository;
import com.autodoc.domain.sheet.SheetColumnType;
import com.autodoc.domain.sheet.SheetRow;
import com.autodoc.domain.sheet.SheetRowRepository;
import com.autodoc.domain.template.DocumentTemplate;
import com.autodoc.domain.template.DocumentTemplateRepository;
import com.autodoc.domain.template.TemplateScope;
import com.autodoc.domain.template.WritingFormat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.seed.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("local")
@Transactional
class DocumentServiceTest {
    private static final Logger log = LoggerFactory.getLogger(DocumentServiceTest.class);
    @Autowired private DocumentService documentService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTemplateRepository templateRepository;
    @Autowired private DocumentFileRepository documentFileRepository;
    @Autowired private SheetColumnRepository sheetColumnRepository;
    @Autowired private SheetRowRepository sheetRowRepository;

    @Test void createsDocument() {
        DocumentTemplate template = basicTemplate("document-create-template");
        DocumentResponse response = documentService.createDocument(1L, request(template.getId(), "문서 생성"));
        assertThat(response.id()).isNotNull();
        log.info("문서 생성 서비스 검증 완료: documentId={}", response.id());
    }

    @Test void getsOnlyMyDocuments() {
        DocumentTemplate template = basicTemplate("document-list-template");
        documentRepository.saveAndFlush(new Document(1L, template.getId(), "내 문서", WritingFormat.DOCS, "report", null, null, null));
        documentRepository.saveAndFlush(new Document(2L, template.getId(), "다른 사용자 문서", WritingFormat.DOCS, "report", null, null, null));
        assertThat(documentService.getMyDocuments(1L)).extracting(item -> item.title()).containsExactly("내 문서");
        log.info("내 문서 목록 조회 서비스 검증 완료");
    }

    @Test void updatesOwnDocument() {
        Document document = documentRepository.saveAndFlush(new Document(1L, basicTemplate("document-update-template").getId(), "수정 전", WritingFormat.DOCS, "report", null, null, null));
        DocumentResponse response = documentService.updateDocument(document.getId(), 1L, new DocumentUpdateRequest("수정 후", "handover", "본문", "중요", "주의"));
        assertThat(response.title()).isEqualTo("수정 후");
        log.info("문서 수정 서비스 검증 완료: documentId={}", document.getId());
    }

    @Test void rejectsOtherUsersDocument() {
        Document document = documentRepository.saveAndFlush(new Document(2L, basicTemplate("document-access-template").getId(), "다른 문서", WritingFormat.DOCS, "report", null, null, null));
        assertThatThrownBy(() -> documentService.getDocument(document.getId(), 1L)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        log.info("타 사용자 문서 접근 권한 검증 완료");
    }

    @Test void deletesDocumentAndRelatedData() {
        Document document = documentRepository.saveAndFlush(new Document(1L, basicTemplate("document-delete-template").getId(), "삭제 문서", WritingFormat.SHEETS, "list", null, null, null));
        documentFileRepository.saveAndFlush(new DocumentFile(document.getId(), 1L, DocumentFileRole.ATTACHMENT, "a.txt", "delete-file", "txt", "text/plain", "/a", 1L, "c"));
        sheetColumnRepository.saveAndFlush(new SheetColumn(document.getId(), "name", "이름", SheetColumnType.TEXT, 1));
        sheetRowRepository.saveAndFlush(new SheetRow(document.getId(), 1, "{\"name\":\"테스트\"}"));
        documentService.deleteDocument(document.getId(), 1L);
        assertThat(documentRepository.findById(document.getId())).isEmpty();
        assertThat(documentFileRepository.findAll()).noneMatch(file -> file.getDocumentId().equals(document.getId()));
        assertThat(sheetColumnRepository.findAll()).noneMatch(column -> column.getDocumentId().equals(document.getId()));
        assertThat(sheetRowRepository.findAll()).noneMatch(row -> row.getDocumentId().equals(document.getId()));
        log.info("문서 및 연결 데이터 삭제 서비스 검증 완료: documentId={}", document.getId());
    }

    private DocumentTemplate basicTemplate(String name) { return templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC, name, WritingFormat.DOCS, "report", "{}", "{}")); }
    private DocumentCreateRequest request(Long templateId, String title) { return new DocumentCreateRequest(templateId, title, WritingFormat.DOCS, "report", "본문", "중요", "주의"); }
}
