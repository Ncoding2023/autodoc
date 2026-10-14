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
import com.autodoc.domain.sheet.SheetService;
import com.autodoc.domain.sheet.dto.SheetCreateRequest;
import com.autodoc.domain.sheet.dto.SheetColumnCreateRequest;
import com.autodoc.domain.sheet.dto.SheetRowRequest;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
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
    @Autowired private SheetService sheetService;
    @Autowired private ObjectMapper objectMapper;

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
        Document own = documentRepository.saveAndFlush(new Document(1L, template.getId(), "내 문서", WritingFormat.DOCS, "report", null, null, null));
        Document other = documentRepository.saveAndFlush(new Document(2L, template.getId(), "다른 사용자 문서", WritingFormat.DOCS, "report", null, null, null));
        var documents = documentService.getMyDocuments(1L);
        assertThat(documents).extracting(item -> item.id()).contains(own.getId()).doesNotContain(other.getId());
        assertThat(documents).allSatisfy(item -> assertThat(documentRepository.findById(item.id()).orElseThrow().getOwnerUserId()).isEqualTo(1L));
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

    @Test
    @DisplayName("SHEETS 문서 생성 시 템플릿의 열 이름·형식·순서를 적용한다")
    void createsSheetWithTemplateColumns() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC,
                "document-sheet-columns-template", WritingFormat.SHEETS, "list",
                "{\"columns\":[{\"key\":\"task\",\"name\":\"업무\",\"type\":\"TEXT\"},{\"key\":\"done\",\"name\":\"완료\",\"type\":\"BOOLEAN\"}]}", "{}"));
        DocumentResponse response = documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "시트 생성", WritingFormat.SHEETS, "list", null, null, null));
        assertThat(sheetColumnRepository.findByDocumentIdOrderByColumnOrder(response.id())).extracting(SheetColumn::getColumnKey).containsExactly("task", "done");
        assertThat(sheetColumnRepository.findByDocumentIdOrderByColumnOrder(response.id())).extracting(SheetColumn::getColumnType).containsExactly(SheetColumnType.TEXT, SheetColumnType.BOOLEAN);
        log.info("SHEETS 문서 생성 시 템플릿 열 적용 검증 완료");
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("템플릿 열 적용 실패 시 문서와 생성된 열을 모두 롤백한다")
    void rollsBackDocumentOnInvalidTemplateColumns() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC,
                "invalid-sheet-template-" + java.util.UUID.randomUUID(), WritingFormat.SHEETS, "list",
                "{\"columns\":[{\"key\":\"task\",\"name\":\"업무\",\"type\":\"TEXT\"},{\"key\":\"task\",\"name\":\"중복 열\",\"type\":\"TEXT\"}]}", "{}"));
        long documentsBefore = documentRepository.count();
        long columnsBefore = sheetColumnRepository.count();
        try {
            assertThatThrownBy(() -> documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "롤백 검증", WritingFormat.SHEETS, "list", null, null, null)))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
            assertThat(documentRepository.count()).isEqualTo(documentsBefore);
            assertThat(sheetColumnRepository.count()).isEqualTo(columnsBefore);
            log.info("잘못된 템플릿 열 적용 시 문서 및 열 저장 롤백 검증 완료");
        } finally { templateRepository.deleteById(template.getId()); }
    }

    @Test
    @DisplayName("시트 생성 시 편집한 열 순서와 입력한 행을 문서와 함께 저장한다")
    void createsDocumentWithEditedSheetDraft() throws Exception {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC,
                "edited-sheet-draft-template", WritingFormat.SHEETS, "list",
                "{\"columns\":[{\"key\":\"old\",\"name\":\"템플릿 열\",\"type\":\"TEXT\"}]}", "{}"));
        SheetCreateRequest sheet = new SheetCreateRequest(List.of(
                new SheetColumnCreateRequest("number", "수량", SheetColumnType.NUMBER),
                new SheetColumnCreateRequest("done", "완료", SheetColumnType.BOOLEAN),
                new SheetColumnCreateRequest("task", "업무", SheetColumnType.TEXT)),
                List.of(new SheetRowRequest(objectMapper.readTree("{\"task\":\"업무 내용\",\"number\":0,\"done\":false}"))));
        var document = documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "한 번에 저장", WritingFormat.SHEETS, "list", null, null, null, sheet));
        assertThat(sheetService.getColumns(document.id(), 1L)).extracting(item -> item.columnKey()).containsExactly("number", "done", "task");
        var row = sheetService.getRows(document.id(), 1L).getFirst();
        assertThat(row.rowData().get("task").asText()).isEqualTo("업무 내용");
        assertThat(row.rowData().get("number").asInt()).isZero();
        assertThat(row.rowData().get("done").asBoolean()).isFalse();
        log.info("문서 생성 시 편집한 열 및 행 일괄 저장 검증 완료");
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("시트 행 저장 실패 시 문서와 이미 저장한 열·행을 모두 롤백한다")
    void rollsBackCompleteSheetDraftOnInvalidRow() throws Exception {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC,
                "atomic-sheet-draft-" + java.util.UUID.randomUUID(), WritingFormat.SHEETS, "list", "{}", "{}"));
        long documentsBefore = documentRepository.count();
        long columnsBefore = sheetColumnRepository.count();
        long rowsBefore = sheetRowRepository.count();
        SheetCreateRequest sheet = new SheetCreateRequest(List.of(new SheetColumnCreateRequest("done", "완료", SheetColumnType.BOOLEAN)),
                List.of(new SheetRowRequest(objectMapper.readTree("{\"done\":true}")), new SheetRowRequest(objectMapper.readTree("{\"done\":\"잘못된 값\"}"))));
        try {
            assertThatThrownBy(() -> documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "전체 롤백", WritingFormat.SHEETS, "list", null, null, null, sheet)))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
            assertThat(documentRepository.count()).isEqualTo(documentsBefore);
            assertThat(sheetColumnRepository.count()).isEqualTo(columnsBefore);
            assertThat(sheetRowRepository.count()).isEqualTo(rowsBefore);
            log.info("시트 행 입력 실패 시 문서·열·행 전체 롤백 검증 완료");
        } finally { templateRepository.deleteById(template.getId()); }
    }

    @Test
    @DisplayName("DOCS 문서 생성에는 시트 데이터를 포함할 수 없다")
    void rejectsSheetDraftForDocs() {
        DocumentTemplate template = basicTemplate("docs-sheet-draft-rejected");
        long before = documentRepository.count();
        assertThatThrownBy(() -> documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "잘못된 생성", WritingFormat.DOCS, "report", null, null, null,
                new SheetCreateRequest(List.of(), List.of()))))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOCUMENT_FORMAT));
        assertThat(documentRepository.count()).isEqualTo(before);
        log.info("DOCS 문서에 시트 생성 데이터 포함 거부 검증 완료");
    }

    @Test
    @DisplayName("시트 수정 시 문서 정보와 변경한 열·행을 함께 저장한다")
    void updatesCompleteSheet() throws Exception {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC,
                "sheet-update-template", WritingFormat.SHEETS, "list", "{}", "{}"));
        var original = new SheetCreateRequest(List.of(new SheetColumnCreateRequest("old", "기존", SheetColumnType.TEXT)),
                List.of(new SheetRowRequest(objectMapper.readTree("{\"old\":\"기존 값\"}"))));
        var document = documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "수정 전", WritingFormat.SHEETS, "list", null, null, null, original));
        var edited = new SheetCreateRequest(List.of(new SheetColumnCreateRequest("done", "완료", SheetColumnType.BOOLEAN),
                new SheetColumnCreateRequest("quantity", "수량", SheetColumnType.NUMBER)),
                List.of(new SheetRowRequest(objectMapper.readTree("{\"done\":false,\"quantity\":0}"))));
        documentService.updateDocument(document.id(), 1L, new DocumentUpdateRequest("수정 후", "list", null, null, null, edited));
        documentRepository.flush();
        assertThat(documentService.getDocument(document.id(), 1L).title()).isEqualTo("수정 후");
        assertThat(sheetService.getColumns(document.id(), 1L)).extracting(column -> column.columnKey()).containsExactly("done", "quantity");
        var rows = sheetService.getRows(document.id(), 1L);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().rowData().get("done").asBoolean()).isFalse();
        assertThat(rows.getFirst().rowData().get("quantity").asInt()).isZero();
        log.info("시트 수정 시 문서 정보와 열·행 일괄 저장 검증 완료");
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("시트 일괄 수정 실패 시 기존 문서와 열·행을 유지한다")
    void preservesSheetOnFailedUpdate() throws Exception {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC,
                "sheet-update-rollback-" + java.util.UUID.randomUUID(), WritingFormat.SHEETS, "list", "{}", "{}"));
        Long documentId = null;
        try {
            var original = new SheetCreateRequest(List.of(new SheetColumnCreateRequest("task", "업무", SheetColumnType.TEXT)),
                    List.of(new SheetRowRequest(objectMapper.readTree("{\"task\":\"원래 값\"}"))));
            var document = documentService.createDocument(1L, new DocumentCreateRequest(template.getId(), "원래 제목", WritingFormat.SHEETS, "list", null, null, null, original));
            documentId = document.id();
            var invalid = new SheetCreateRequest(List.of(new SheetColumnCreateRequest("done", "완료", SheetColumnType.BOOLEAN)),
                    List.of(new SheetRowRequest(objectMapper.readTree("{\"done\":true}")), new SheetRowRequest(objectMapper.readTree("{\"done\":\"오류\"}"))));
            assertThatThrownBy(() -> documentService.updateDocument(document.id(), 1L, new DocumentUpdateRequest("변경 제목", "list", null, null, null, invalid)))
                    .isInstanceOf(BusinessException.class);
            assertThat(documentService.getDocument(document.id(), 1L).title()).isEqualTo("원래 제목");
            assertThat(sheetService.getColumns(document.id(), 1L)).extracting(column -> column.columnKey()).containsExactly("task");
            assertThat(sheetService.getRows(document.id(), 1L)).hasSize(1);
            assertThat(sheetService.getRows(document.id(), 1L).getFirst().rowData().get("task").asText()).isEqualTo("원래 값");
            log.info("시트 수정 실패 시 기존 데이터 복구 검증 완료");
        } finally {
            if (documentId != null) documentService.deleteDocument(documentId, 1L);
            templateRepository.deleteById(template.getId());
        }
    }

    private DocumentTemplate basicTemplate(String name) { return templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC, name, WritingFormat.DOCS, "report", "{}", "{}")); }
    private DocumentCreateRequest request(Long templateId, String title) { return new DocumentCreateRequest(templateId, title, WritingFormat.DOCS, "report", "본문", "중요", "주의"); }
}
