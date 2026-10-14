package com.autodoc.domain.sheet;

import com.autodoc.common.exception.*;
import com.autodoc.domain.document.*;
import com.autodoc.domain.sheet.dto.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import jakarta.validation.Validator;

@Service
@Transactional(readOnly = true)
public class SheetService {
    private final DocumentRepository documentRepository;
    private final SheetColumnRepository columnRepository;
    private final SheetRowRepository rowRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SheetService(DocumentRepository d, SheetColumnRepository c, SheetRowRepository r, ObjectMapper o, Validator v) {
        documentRepository = d;
        columnRepository = c;
        rowRepository = r;
        objectMapper = o;
        validator = v;
    }

    @Transactional
    public SheetColumnResponse addColumn(Long documentId, Long userId, SheetColumnCreateRequest r) {
        verifyForMutation(documentId, userId);
        if (!validator.validate(r).isEmpty()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        List<SheetColumn> columns = columnRepository.findByDocumentIdOrderByColumnOrder(documentId);
        if (columns.stream().anyMatch(c -> c.getColumnKey().equals(r.columnKey())))
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        int order = columns.stream().mapToInt(SheetColumn::getColumnOrder).max().orElse(0) + 1;

        return SheetColumnResponse.from(columnRepository.save(new SheetColumn(documentId, r.columnKey(), r.columnName(), r.columnType(), order)));
    }

    public List<SheetColumnResponse> getColumns(Long documentId, Long userId) {
        verify(documentId, userId);

        return columnRepository.findByDocumentIdOrderByColumnOrder(documentId).stream().map(SheetColumnResponse::from).toList();
    }

    @Transactional
    public SheetColumnResponse updateColumn(Long documentId, Long columnId, Long userId, SheetColumnUpdateRequest r) {
        verifyForMutation(documentId, userId);
        if (!validator.validate(r).isEmpty()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        SheetColumn c = column(columnId, documentId);
        if (c.getColumnType() != r.columnType()) {
            for (SheetRow row : rowRepository.findByDocumentIdOrderByRowNo(documentId)) {
                JsonNode data = response(row).rowData();
                JsonNode value = data.get(c.getColumnKey());
                if (value != null && value.isTextual() && value.asText().isEmpty() && data instanceof ObjectNode object) {
                    object.putNull(c.getColumnKey());
                    row.updateRowData(json(object));
                } else validateValue(value, r.columnType());
            }
        }
        c.update(r.columnName(), r.columnType());
        return SheetColumnResponse.from(c);
    }

    @Transactional
    public List<SheetColumnResponse> updateColumnOrder(Long documentId, Long userId, SheetColumnOrderRequest r) {
        verifyForMutation(documentId, userId);
        List<SheetColumn> columns = columnRepository.findByDocumentIdOrderByColumnOrder(documentId);
        Set<Long> requestedIds = new HashSet<>(r.columnIds());
        if (requestedIds.size() != columns.size() || r.columnIds().size() != columns.size() || !columns.stream().map(SheetColumn::getId).collect(java.util.stream.Collectors.toSet()).equals(requestedIds))
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Map<Long, SheetColumn> columnsById = columns.stream().collect(java.util.stream.Collectors.toMap(SheetColumn::getId, c -> c));
        for (int index = 0; index < columns.size(); index++) columns.get(index).updateOrder(-(index + 1));
        columnRepository.flush();
        for (int index = 0; index < r.columnIds().size(); index++)
            columnsById.get(r.columnIds().get(index)).updateOrder(index + 1);
        return r.columnIds().stream().map(columnsById::get).map(SheetColumnResponse::from).toList();
    }

    @Transactional
    public void deleteColumn(Long documentId, Long columnId, Long userId) {
        verifyForMutation(documentId, userId);
        SheetColumn c = column(columnId, documentId);
        for (SheetRow row : rowRepository.findByDocumentIdOrderByRowNo(documentId)) {
            try {
                JsonNode n = objectMapper.readTree(row.getRowData());
                if (n instanceof ObjectNode o) {
                    o.remove(c.getColumnKey());
                    row.updateRowData(objectMapper.writeValueAsString(o));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        columnRepository.delete(c);
    }

    @Transactional
    public SheetRowResponse addRow(Long documentId, Long userId, SheetRowRequest r) {
        verifyForMutation(documentId, userId);
        validateRowData(documentId, r.rowData());
        int rowNo = rowRepository.findByDocumentIdOrderByRowNo(documentId).stream().mapToInt(SheetRow::getRowNo).max().orElse(0) + 1;
        SheetRow row = new SheetRow(documentId, rowNo, json(r.rowData()));
        return response(rowRepository.save(row));
    }

    public List<SheetRowResponse> getRows(Long documentId, Long userId) {
        verify(documentId, userId);
        return rowRepository.findByDocumentIdOrderByRowNo(documentId).stream().map(this::response).toList();
    }

    @Transactional
    public SheetRowResponse updateRow(Long documentId, Long rowId, Long userId, SheetRowRequest r) {
        verifyForMutation(documentId, userId);
        SheetRow row = row(rowId, documentId);
        validateRowData(documentId, r.rowData());
        row.updateRowData(json(r.rowData()));
        return response(row);
    }

    @Transactional
    public void deleteRow(Long documentId, Long rowId, Long userId) {
        verifyForMutation(documentId, userId);
        rowRepository.delete(row(rowId, documentId));
    }

    private void verify(Long id, Long user) {
        Document d = documentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        verifyDocument(d, user);
    }

    private void verifyForMutation(Long id, Long user) {
        Document d = documentRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        verifyDocument(d, user);
    }

    private void verifyDocument(Document d, Long user) {
        if (!d.getOwnerUserId().equals(user)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        if (d.getWritingFormat() != com.autodoc.domain.template.WritingFormat.SHEETS)
            throw new BusinessException(ErrorCode.INVALID_DOCUMENT_FORMAT);
    }

    @Transactional
    public void replaceSheet(Long documentId, Long userId, SheetCreateRequest request) {
        verifyForMutation(documentId, userId);
        if (!validator.validate(request).isEmpty()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        rowRepository.deleteByDocumentId(documentId);
        columnRepository.deleteByDocumentId(documentId);
        // 기존 순서와 키의 유일성 제약을 해제한 뒤 새 구성을 저장한다.
        rowRepository.flush();
        columnRepository.flush();
        createSheet(documentId, userId, request);
    }

    @Transactional
    public void createSheet(Long documentId, Long userId, SheetCreateRequest request) {
        verifyForMutation(documentId, userId);
        if (!validator.validate(request).isEmpty()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        for (SheetColumnCreateRequest column : request.columns()) addColumn(documentId, userId, column);
        for (SheetRowRequest row : request.rows()) addRow(documentId, userId, row);
    }

    @Transactional
    public void initializeTemplateColumns(Long documentId, Long userId, String structureData) {
        verifyForMutation(documentId, userId);
        JsonNode structure;
        try { structure = objectMapper.readTree(structureData); }
        catch (Exception e) { throw new BusinessException(ErrorCode.INVALID_REQUEST); }
        if (structure == null || !structure.isObject()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        JsonNode columns = structure.get("columns");
        if (columns == null) return;
        if (!columns.isArray()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        for (JsonNode definition : columns) {
            if (!definition.isObject() || !definition.path("key").isTextual() || !definition.path("name").isTextual() || !definition.path("type").isTextual())
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            SheetColumnType type;
            try { type = SheetColumnType.valueOf(definition.get("type").asText()); }
            catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.INVALID_REQUEST); }
            addColumn(documentId, userId, new SheetColumnCreateRequest(definition.get("key").asText(), definition.get("name").asText(), type));
        }
    }

    private void validateRowData(Long documentId, JsonNode data) {
        if (data == null || !data.isObject()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Map<String, SheetColumn> columns = columnRepository.findByDocumentIdOrderByColumnOrder(documentId).stream()
                .collect(java.util.stream.Collectors.toMap(SheetColumn::getColumnKey, c -> c));
        if (columns.isEmpty()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        for (var field : data.properties()) {
            SheetColumn column = columns.get(field.getKey());
            if (column == null) throw new BusinessException(ErrorCode.INVALID_REQUEST);
            validateValue(field.getValue(), column.getColumnType());
        }
    }

    private void validateValue(JsonNode value, SheetColumnType type) {
        if (value == null || value.isNull()) return;
        boolean valid = switch (type) {
            case TEXT -> value.isTextual();
            case NUMBER -> value.isNumber() && Double.isFinite(value.asDouble());
            case BOOLEAN -> value.isBoolean();
            case DATE -> isDate(value);
        };
        if (!valid) throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private boolean isDate(JsonNode value) {
        if (!value.isTextual() || !value.asText().matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try { LocalDate.parse(value.asText()); return true; }
        catch (DateTimeParseException e) { return false; }
    }

    private SheetColumn column(Long id, Long doc) {
        SheetColumn c = columnRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SHEET_COLUMN_NOT_FOUND));
        if (!c.getDocumentId().equals(doc)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return c;
    }

    private SheetRow row(Long id, Long doc) {
        SheetRow r = rowRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SHEET_ROW_NOT_FOUND));
        if (!r.getDocumentId().equals(doc)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return r;
    }

    private String json(JsonNode n) {
        try {
            return objectMapper.writeValueAsString(n);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private SheetRowResponse response(SheetRow r) {
        try {
            return new SheetRowResponse(r.getId(), r.getRowNo(), objectMapper.readTree(r.getRowData()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
