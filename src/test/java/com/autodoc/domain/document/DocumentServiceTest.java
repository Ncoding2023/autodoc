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
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("문서 서비스")
class DocumentServiceTest {
    private static final Logger log = LoggerFactory.getLogger(DocumentServiceTest.class);
    @Autowired private DocumentService documentService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTemplateRepository templateRepository;
    @Autowired private DocumentFileRepository documentFileRepository;
    @Autowired private SheetColumnRepository sheetColumnRepository;
    @Autowired private SheetRowRepository sheetRowRepository;

    @Test
    @DisplayName("다른 회원의 사용자 템플릿으로 문서를 생성할 수 없다")
    void rejectsOtherUsersTemplateOnCreate() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(2L,
                "document-other-user-template", WritingFormat.DOCS, "report", "{}", "{}"));
        assertThatThrownBy(() -> documentService.createDocument(1L, request(template.getId(), "접근 불가 템플릿")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        log.info("다른 회원의 템플릿을 사용한 문서 생성 거부 검증 완료");
    }

    @Test
    @DisplayName("다른 회원의 문서를 수정하거나 삭제할 수 없다")
    void rejectsOtherUsersDocumentMutations() {
        Document document = documentRepository.saveAndFlush(new Document(2L,
                basicTemplate("document-mutation-permission-template").getId(), "원래 문서",
                WritingFormat.DOCS, "report", "원래 본문", null, null));
        assertThatThrownBy(() -> documentService.updateDocument(document.getId(), 1L,
                new DocumentUpdateRequest("변경 시도", "report", "변경 본문", null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> documentService.deleteDocument(document.getId(), 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        assertThat(documentRepository.findById(document.getId())).isPresent();
        assertThat(document.getTitle()).isEqualTo("원래 문서");
        assertThat(document.getBodyContent()).isEqualTo("원래 본문");
        log.info("다른 회원의 문서 수정 및 삭제 거부 검증 완료");
    }

    @Test
    @DisplayName("템플릿과 작성 형식이 다른 문서는 생성할 수 없다")
    void rejectsMismatchedTemplateFormat() {
        DocumentTemplate template = basicTemplate("document-mismatched-format");
        long countBefore = documentRepository.count();
        assertThatThrownBy(() -> documentService.createDocument(1L,
                new DocumentCreateRequest(template.getId(), "형식 불일치", WritingFormat.SHEETS, "list", null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOCUMENT_FORMAT));
        assertThat(documentRepository.count()).isEqualTo(countBefore);
        log.info("템플릿과 문서 작성 형식 불일치 거부 검증 완료");
    }

    @Test
    @DisplayName("문서를 생성한다")
    void createsDocument() {
        DocumentTemplate template = basicTemplate("document-create-template");
        DocumentResponse response = documentService.createDocument(1L, request(template.getId(), "문서 생성"));
        assertThat(response.id()).isNotNull();
        log.info("문서 생성 서비스 검증 완료: documentId={}", response.id());
    }

    @Test
    @DisplayName("문서 목록에 본인 소유 문서만 포함한다")
    void getsOnlyMyDocuments() {
        DocumentTemplate template = basicTemplate("document-list-template");
        documentRepository.saveAndFlush(new Document(1L, template.getId(), "내 문서", WritingFormat.DOCS, "report", null, null, null));
        documentRepository.saveAndFlush(new Document(2L, template.getId(), "다른 사용자 문서", WritingFormat.DOCS, "report", null, null, null));
        assertThat(documentService.getMyDocuments(1L)).extracting(item -> item.title()).containsExactly("내 문서");
        log.info("내 문서 목록 조회 서비스 검증 완료");
    }

    @Test
    @DisplayName("본인 소유 문서를 수정한다")
    void updatesOwnDocument() {
        Document document = documentRepository.saveAndFlush(new Document(1L, basicTemplate("document-update-template").getId(), "수정 전", WritingFormat.DOCS, "report", null, null, null));
        DocumentResponse response = documentService.updateDocument(document.getId(), 1L, new DocumentUpdateRequest("수정 후", "handover", "본문", "중요", "주의"));
        assertThat(response.title()).isEqualTo("수정 후");
        assertThat(response.documentType()).isEqualTo("handover");
        assertThat(response.bodyContent()).isEqualTo("본문");
        assertThat(response.importantNotes()).isEqualTo("중요");
        assertThat(response.cautions()).isEqualTo("주의");
        log.info("문서 수정 서비스 검증 완료: documentId={}", document.getId());
    }

    @Test
    @DisplayName("다른 회원의 문서에 접근할 수 없다")
    void rejectsOtherUsersDocument() {
        Document document = documentRepository.saveAndFlush(new Document(2L, basicTemplate("document-access-template").getId(), "다른 문서", WritingFormat.DOCS, "report", null, null, null));
        assertThatThrownBy(() -> documentService.getDocument(document.getId(), 1L)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        log.info("타 사용자 문서 접근 권한 검증 완료");
    }

    @Test
    @DisplayName("문서 삭제 시 파일과 SHEETS 열 및 행도 삭제한다")
    void deletesDocumentAndRelatedData() {
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
