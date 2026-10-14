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
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("SHEETS 서비스")
class SheetServiceTest {
    private static final Logger log = LoggerFactory.getLogger(SheetServiceTest.class);

    @Autowired
    private SheetService sheetService;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DocumentTemplateRepository templateRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("SHEETS 열 추가 시 열 키와 순서를 설정한다")
    void addsColumn() {
        Document document = sheetsDocument("열 추가");

        var response = sheetService.addColumn(document.getId(), 1L, column("task", "업무"));

        assertThat(response.columnKey()).isEqualTo("task");
        assertThat(response.columnOrder()).isEqualTo(1);
        log.info("SHEETS 열 추가 서비스 검증 완료: columnId={}", response.id());
    }

    @Test
    @DisplayName("SHEETS 열을 지정된 순서대로 조회한다")
    void getsColumnsInOrder() {
        Document document = sheetsDocument("열 목록");
        sheetService.addColumn(document.getId(), 1L, column("task", "업무"));
        sheetService.addColumn(document.getId(), 1L, column("owner", "담당자"));

        assertThat(sheetService.getColumns(document.getId(), 1L))
                .extracting(item -> item.columnKey())
                .containsExactly("task", "owner");
        log.info("SHEETS 열 목록 조회 서비스 검증 완료: documentId={}", document.getId());
    }

    @Test
    @DisplayName("SHEETS 열 이름을 수정한다")
    void updatesColumn() {
        Document document = sheetsDocument("열 수정");
        Long columnId = sheetService.addColumn(document.getId(), 1L, column("task", "업무")).id();

        var response = sheetService.updateColumn(document.getId(), columnId, 1L,
                new SheetColumnUpdateRequest("업무 내용", SheetColumnType.TEXT));

        assertThat(response.columnName()).isEqualTo("업무 내용");
        log.info("SHEETS 열 수정 서비스 검증 완료: columnId={}", columnId);
    }

    @Test
    @DisplayName("SHEETS 열 순서를 변경한다")
    void changesColumnOrder() {
        Document document = sheetsDocument("열 순서 변경");
        Long firstId = sheetService.addColumn(document.getId(), 1L, column("task", "업무")).id();
        Long secondId = sheetService.addColumn(document.getId(), 1L, column("owner", "담당자")).id();

        var response = sheetService.updateColumnOrder(document.getId(), 1L,
                new SheetColumnOrderRequest(List.of(secondId, firstId)));

        assertThat(response).extracting(item -> item.columnKey()).containsExactly("owner", "task");
        log.info("SHEETS 열 순서 변경 서비스 검증 완료: documentId={}", document.getId());
    }

    @Test
    @DisplayName("SHEETS 열 삭제 시 해당 열의 행 데이터만 제거한다")
    void deletesColumnAndRemovesItsRowData() throws Exception {
        Document document = sheetsDocument("열 삭제");
        Long columnId = sheetService.addColumn(document.getId(), 1L, column("task", "업무")).id();
        sheetService.addColumn(document.getId(), 1L, column("owner", "담당자"));
        sheetService.addRow(document.getId(), 1L, new SheetRowRequest(objectMapper.readTree("{\"task\":\"테스트\",\"owner\":\"관리자\"}")));

        sheetService.deleteColumn(document.getId(), columnId, 1L);

        SheetRowResponse row = sheetService.getRows(document.getId(), 1L).getFirst();
        assertThat(row.rowData().has("task")).isFalse();
        assertThat(row.rowData().get("owner").asText()).isEqualTo("관리자");
        log.info("SHEETS 열 삭제 및 행 데이터 정리 서비스 검증 완료: columnId={}", columnId);
    }

    @Test
    @DisplayName("SHEETS 행 추가 시 행 번호를 설정한다")
    void addsRow() throws Exception {
        Document document = sheetsDocument("행 추가");
        sheetService.addColumn(document.getId(), 1L, column("task", "업무"));

        var response = sheetService.addRow(document.getId(), 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"테스트\"}")));

        assertThat(response.rowNo()).isEqualTo(1);
        log.info("SHEETS 행 추가 서비스 검증 완료: rowId={}", response.id());
    }

    @Test
    @DisplayName("SHEETS 행 데이터를 수정한다")
    void updatesRow() throws Exception {
        Document document = sheetsDocument("행 수정");
        sheetService.addColumn(document.getId(), 1L, column("task", "업무"));
        Long rowId = sheetService.addRow(document.getId(), 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"수정 전\"}"))).id();

        var response = sheetService.updateRow(document.getId(), rowId, 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"수정 후\"}")));

        assertThat(response.rowData().get("task").asText()).isEqualTo("수정 후");
        log.info("SHEETS 행 수정 서비스 검증 완료: rowId={}", rowId);
    }

    @Test
    @DisplayName("SHEETS 행을 삭제한다")
    void deletesRow() throws Exception {
        Document document = sheetsDocument("행 삭제");
        sheetService.addColumn(document.getId(), 1L, column("task", "업무"));
        Long rowId = sheetService.addRow(document.getId(), 1L,
                new SheetRowRequest(objectMapper.readTree("{\"task\":\"삭제 대상\"}"))).id();

        sheetService.deleteRow(document.getId(), rowId, 1L);

        assertThat(sheetService.getRows(document.getId(), 1L)).isEmpty();
        log.info("SHEETS 행 삭제 서비스 검증 완료: rowId={}", rowId);
    }

    @Test
    @DisplayName("DOCS 문서에서는 SHEETS 기능을 사용할 수 없다")
    void rejectsDocsDocument() {
        Document document = documentRepository.saveAndFlush(new Document(1L, template("DOCS", WritingFormat.DOCS).getId(),
                "DOCS 문서", WritingFormat.DOCS, "report", null, null, null));

        assertThatThrownBy(() -> sheetService.getColumns(document.getId(), 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOCUMENT_FORMAT));
        log.info("DOCS 문서의 SHEETS 기능 차단 검증 완료: documentId={}", document.getId());
    }

    @Test
    @DisplayName("중간 열과 행을 삭제한 후 추가해도 순서 번호가 충돌하지 않는다")
    void appendsAfterMiddleDeletion() throws Exception {
        Document document = sheetsDocument("중간 삭제 후 추가");
        sheetService.addColumn(document.getId(), 1L, column("first", "첫 열"));
        Long middle = sheetService.addColumn(document.getId(), 1L, column("middle", "중간 열")).id();
        sheetService.addColumn(document.getId(), 1L, column("last", "마지막 열"));
        sheetService.deleteColumn(document.getId(), middle, 1L);
        assertThat(sheetService.addColumn(document.getId(), 1L, column("new", "새 열")).columnOrder()).isEqualTo(4);
        SheetRowRequest empty = new SheetRowRequest(objectMapper.readTree("{}"));
        sheetService.addRow(document.getId(), 1L, empty);
        Long middleRow = sheetService.addRow(document.getId(), 1L, empty).id();
        sheetService.addRow(document.getId(), 1L, empty);
        sheetService.deleteRow(document.getId(), middleRow, 1L);
        assertThat(sheetService.addRow(document.getId(), 1L, empty).rowNo()).isEqualTo(4);
        assertThat(sheetService.getRows(document.getId(), 1L)).extracting(SheetRowResponse::rowNo).containsExactly(1, 3, 4);
        log.info("중간 열 및 행 삭제 후 번호 충돌 방지 검증 완료");
    }

    @Test
    @DisplayName("중복 열 키와 최대 길이를 초과한 열 이름은 거부한다")
    void rejectsInvalidColumnInput() {
        Document document = sheetsDocument("열 입력 검증");
        sheetService.addColumn(document.getId(), 1L, column("task", "업무"));
        assertInvalid(() -> sheetService.addColumn(document.getId(), 1L, column("task", "다른 업무")));
        assertInvalid(() -> sheetService.addColumn(document.getId(), 1L, column("another", "가".repeat(101))));
        assertThat(sheetService.getColumns(document.getId(), 1L)).hasSize(1);
        log.info("중복 열 키 및 열 이름 최대 길이 검증 완료");
    }

    @Test
    @DisplayName("문자·숫자·날짜·예아니오 값과 빈 값을 저장한다")
    void savesTypedValuesAndNulls() throws Exception {
        Document document = sheetsDocument("형식별 행 저장");
        typedColumns(document);
        var row = sheetService.addRow(document.getId(), 1L, new SheetRowRequest(objectMapper.readTree("{\"text\":\"업무\",\"number\":1.5,\"date\":\"2026-10-15\",\"boolean\":false}")));
        assertThat(row.rowData().get("number").asDouble()).isEqualTo(1.5);
        assertThat(row.rowData().get("boolean").asBoolean()).isFalse();
        var updated = sheetService.updateRow(document.getId(), row.id(), 1L, new SheetRowRequest(objectMapper.readTree("{\"text\":null,\"number\":0,\"date\":null,\"boolean\":true}")));
        assertThat(updated.rowData().get("number").asInt()).isZero();
        assertThat(updated.rowData().get("date").isNull()).isTrue();
        assertThat(updated.rowData().get("boolean").asBoolean()).isTrue();
        log.info("SHEETS 형식별 행 데이터 및 빈 값 저장 검증 완료");
    }

    @Test
    @DisplayName("객체가 아닌 행·알 수 없는 열·형식에 맞지 않는 값은 거부한다")
    void rejectsInvalidRowData() throws Exception {
        Document document = sheetsDocument("행 형식 거부");
        typedColumns(document);
        for (String json : List.of("[]", "{\"unknown\":1}", "{\"number\":\"123\"}", "{\"date\":\"2026-02-30\"}", "{\"boolean\":\"false\"}", "{\"text\":10}")) {
            SheetRowRequest request = new SheetRowRequest(objectMapper.readTree(json));
            assertInvalid(() -> sheetService.addRow(document.getId(), 1L, request));
        }
        assertThat(sheetService.getRows(document.getId(), 1L)).isEmpty();
        log.info("SHEETS 행 구조 및 형식 불일치 거부 검증 완료");
    }

    @Test
    @DisplayName("열 형식 변경은 기존 값과 호환되는 경우에만 허용한다")
    void validatesColumnTypeChanges() throws Exception {
        Document document = sheetsDocument("열 형식 변경");
        var column = sheetService.addColumn(document.getId(), 1L, column("task", "업무"));
        var empty = sheetService.addColumn(document.getId(), 1L, column("empty", "빈 열"));
        sheetService.addRow(document.getId(), 1L, new SheetRowRequest(objectMapper.readTree("{\"task\":\"문자 값\"}")));
        sheetService.addRow(document.getId(), 1L, new SheetRowRequest(objectMapper.readTree("{\"empty\":\"\"}")));
        assertInvalid(() -> sheetService.updateColumn(document.getId(), column.id(), 1L, new SheetColumnUpdateRequest("업무", SheetColumnType.NUMBER)));
        assertThat(sheetService.getColumns(document.getId(), 1L).getFirst().columnType()).isEqualTo(SheetColumnType.TEXT);
        assertThat(sheetService.updateColumn(document.getId(), empty.id(), 1L, new SheetColumnUpdateRequest("숫자", SheetColumnType.NUMBER)).columnType()).isEqualTo(SheetColumnType.NUMBER);
        assertThat(sheetService.getRows(document.getId(), 1L).get(1).rowData().get("empty").isNull()).isTrue();
        log.info("기존 행 데이터와 열 형식 변경 호환성 검증 완료");
    }

    @Test
    @DisplayName("열 순서 변경 시 중복되거나 누락된 열 ID는 거부한다")
    void rejectsInvalidColumnOrder() {
        Document document = sheetsDocument("잘못된 열 순서");
        var first = sheetService.addColumn(document.getId(), 1L, column("first", "첫 열"));
        var second = sheetService.addColumn(document.getId(), 1L, column("second", "둘째 열"));
        assertInvalid(() -> sheetService.updateColumnOrder(document.getId(), 1L, new SheetColumnOrderRequest(List.of(first.id(), first.id()))));
        assertInvalid(() -> sheetService.updateColumnOrder(document.getId(), 1L, new SheetColumnOrderRequest(List.of(first.id(), Long.MAX_VALUE))));
        assertThat(sheetService.getColumns(document.getId(), 1L)).extracting(item -> item.id()).containsExactly(first.id(), second.id());
        log.info("잘못된 열 순서 변경 요청 거부 검증 완료");
    }

    @Test
    @DisplayName("다른 회원의 시트와 다른 문서의 열·행에는 접근할 수 없다")
    void rejectsForeignDocumentAndCells() throws Exception {
        Document own = sheetsDocument("내 시트");
        Document other = documentRepository.saveAndFlush(new Document(2L, own.getTemplateId(), "다른 시트", WritingFormat.SHEETS, "list", null, null, null));
        var foreignColumn = sheetService.addColumn(other.getId(), 2L, column("task", "업무"));
        var foreignRow = sheetService.addRow(other.getId(), 2L, new SheetRowRequest(objectMapper.readTree("{\"task\":\"원래 값\"}")));
        assertThatThrownBy(() -> sheetService.getColumns(other.getId(), 1L)).isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> sheetService.updateColumn(own.getId(), foreignColumn.id(), 1L, new SheetColumnUpdateRequest("변경", SheetColumnType.TEXT))).isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> sheetService.deleteRow(own.getId(), foreignRow.id(), 1L)).isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        assertThat(sheetService.getRows(other.getId(), 2L).getFirst().rowData().get("task").asText()).isEqualTo("원래 값");
        log.info("다른 회원 및 다른 문서의 시트 데이터 접근 차단 검증 완료");
    }

    private void typedColumns(Document document) {
        for (SheetColumnType type : SheetColumnType.values())
            sheetService.addColumn(document.getId(), 1L, new SheetColumnCreateRequest(type.name().toLowerCase(), type.name(), type));
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
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
