package com.autodoc.domain.sheet;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.document.Document;
import com.autodoc.domain.document.DocumentRepository;
import com.autodoc.domain.sheet.dto.SheetColumnCreateRequest;
import com.autodoc.domain.sheet.dto.SheetColumnOrderRequest;
import com.autodoc.domain.sheet.dto.SheetColumnUpdateRequest;
import com.autodoc.domain.sheet.dto.SheetRowRequest;
import com.autodoc.domain.sheet.dto.SheetRowResponse;
import com.autodoc.domain.template.DocumentTemplate;
import com.autodoc.domain.template.DocumentTemplateRepository;
import com.autodoc.domain.template.TemplateScope;
import com.autodoc.domain.template.WritingFormat;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.seed.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("local")
@Transactional
class SheetServiceTest {
    private static final Logger log = LoggerFactory.getLogger(SheetServiceTest.class);

    @Autowired private SheetService sheetService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentTemplateRepository templateRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test void addsColumn() {
        Document document = sheetsDocument("열 추가");

        var response = sheetService.addColumn(document.getId(), 1L, column("task", "업무"));

        assertThat(response.columnKey()).isEqualTo("task");
        assertThat(response.columnOrder()).isEqualTo(1);
        log.info("SHEETS 열 추가 서비스 검증 완료: columnId={}", response.id());
    }

    @Test void  getsColumnsInOrder() {
        Document document = sheetsDocument("열 목록");
        sheetService.addColumn(document.getId(), 1L, column("task", "업무"));
        sheetService.addColumn(document.getId(), 1L, column("owner", "담당자"));

        assertThat(sheetService.getColumns(document.getId(), 1L))
                .extracting(item -> item.columnKey())
                .containsExactly("task", "owner");
        log.info("SHEETS 열 목록 조회 서비스 검증 완료: documentId={}", document.getId());
    }

    @Test void updatesColumn() {
        Document document = sheetsDocument("열 수정");
        Long columnId = sheetService.addColumn(document.getId(), 1L, column("task", "업무")).id();

        var response = sheetService.updateColumn(document.getId(), columnId, 1L,
                new SheetColumnUpdateRequest("업무 내용", SheetColumnType.TEXT));

        assertThat(response.columnName()).isEqualTo("업무 내용");
        log.info("SHEETS 열 수정 서비스 검증 완료: columnId={}", columnId);
    }

    @Test void changesColumnOrder() {
        Document document = sheetsDocument("열 순서 변경");
        Long firstId = sheetService.addColumn(document.getId(), 1L, column("task", "업무")).id();
        Long secondId = sheetService.addColumn(document.getId(), 1L, column("owner", "담당자")).id();

        var response = sheetService.updateColumnOrder(document.getId(), 1L,
                new SheetColumnOrderRequest(List.of(secondId, firstId)));

        assertThat(response).extracting(item -> item.columnKey()).containsExactly("owner", "task");
        log.info("SHEETS 열 순서 변경 서비스 검증 완료: documentId={}", document.getId());
    }

    @Test void deletesColumnAndRemovesItsRowData() throws Exception {
        Document document = sheetsDocument("열 삭제");
        Long columnId = sheetService.addColumn(document.getId(), 1L, column("task", "업무")).id();
        sheetService.addRow(document.getId(), 1L, new SheetRowRequest(objectMapper.readTree("{\"task\":\"테스트\",\"owner\":\"관리자\"}")));

        sheetService.deleteColumn(document.getId(), columnId, 1L);

        SheetRowResponse row = sheetService.getRows(document.getId(), 1L).getFirst();
        assertThat(row.rowData().has("task")).isFalse();
        assertThat(row.rowData().get("owner").asText()).isEqualTo("관리자");
        log.info("SHEETS 열 삭제 및 행 데이터 정리 서비스 검증 완료: columnId={}", columnId);
    }

    @Test void addsRow() throws Exception {
        Document document = sheetsDocument("행 추가");

        var response = sheetService.addRow(document.getId(), 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"테스트\"}")));

        assertThat(response.rowNo()).isEqualTo(1);
        log.info("SHEETS 행 추가 서비스 검증 완료: rowId={}", response.id());
    }

    @Test void updatesRow() throws Exception {
        Document document = sheetsDocument("행 수정");
        Long rowId = sheetService.addRow(document.getId(), 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"수정 전\"}"))).id();

        var response = sheetService.updateRow(document.getId(), rowId, 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"수정 후\"}")));

        assertThat(response.rowData().get("task").asText()).isEqualTo("수정 후");
        log.info("SHEETS 행 수정 서비스 검증 완료: rowId={}", rowId);
    }

    @Test void deletesRow() throws Exception {
        Document document = sheetsDocument("행 삭제");
        Long rowId = sheetService.addRow(document.getId(), 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"삭제 대상\"}"))).id();

        sheetService.deleteRow(document.getId(), rowId, 1L);

        assertThat(sheetService.getRows(document.getId(), 1L)).isEmpty();
        log.info("SHEETS 행 삭제 서비스 검증 완료: rowId={}", rowId);
    }

    @Test void rejectsDocsDocument() {
        Document document = documentRepository.saveAndFlush(new Document(1L, template("DOCS", WritingFormat.DOCS).getId(),
                "DOCS 문서", WritingFormat.DOCS, "report", null, null, null));

        assertThatThrownBy(() -> sheetService.getColumns(document.getId(), 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOCUMENT_FORMAT));
        log.info("DOCS 문서의 SHEETS 기능 차단 검증 완료: documentId={}", document.getId());
    }

    private SheetColumnCreateRequest column(String key, String name) {
        return new SheetColumnCreateRequest(key, name, SheetColumnType.TEXT);
    }

    private Document sheetsDocument(String title) {
        DocumentTemplate template = template(title, WritingFormat.SHEETS);
        return documentRepository.saveAndFlush(new Document(1L, template.getId(), title,
                WritingFormat.SHEETS, "list", null, null, null));
    }

    private DocumentTemplate template(String name, WritingFormat writingFormat) {
        return templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC, "sheet-service-" + name,
                writingFormat, "list", "{}", "{}"));
    }
}
