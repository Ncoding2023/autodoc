package com.autodoc.domain.sheet;

import com.autodoc.common.exception.*;
import com.autodoc.domain.document.*;
import com.autodoc.domain.sheet.dto.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class SheetService {
    private final DocumentRepository documentRepository;
    private final SheetColumnRepository columnRepository;
    private final SheetRowRepository rowRepository;
    private final ObjectMapper objectMapper;

    public SheetService(DocumentRepository d, SheetColumnRepository c, SheetRowRepository r, ObjectMapper o) {
        documentRepository = d;
        columnRepository = c;
        rowRepository = r;
        objectMapper = o;
    }

    @Transactional
    public SheetColumnResponse addColumn(Long documentId, Long userId, SheetColumnCreateRequest r) {
        verify(documentId, userId);

        int order = columnRepository.findByDocumentIdOrderByColumnOrder(documentId).size() + 1;

        return SheetColumnResponse.from(columnRepository.save(new SheetColumn(documentId, r.columnKey(), r.columnName(), r.columnType(), order)));
    }

    public List<SheetColumnResponse> getColumns(Long documentId, Long userId) {
        verify(documentId, userId);

        return columnRepository.findByDocumentIdOrderByColumnOrder(documentId).stream().map(SheetColumnResponse::from).toList();
    }

    @Transactional
    public SheetColumnResponse updateColumn(Long documentId, Long columnId, Long userId, SheetColumnUpdateRequest r) {
        verify(documentId, userId);
        SheetColumn c = column(columnId, documentId);
        c.update(r.columnName(), r.columnType());
        return SheetColumnResponse.from(c);
    }

    @Transactional
    public List<SheetColumnResponse> updateColumnOrder(Long documentId, Long userId, SheetColumnOrderRequest r) {
        verify(documentId, userId);
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
        verify(documentId, userId);
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
        verify(documentId, userId);
        SheetRow row = new SheetRow(documentId, rowRepository.findByDocumentIdOrderByRowNo(documentId).size() + 1, json(r.rowData()));
        return response(rowRepository.save(row));
    }

    public List<SheetRowResponse> getRows(Long documentId, Long userId) {
        verify(documentId, userId);
        return rowRepository.findByDocumentIdOrderByRowNo(documentId).stream().map(this::response).toList();
    }

    @Transactional
    public SheetRowResponse updateRow(Long documentId, Long rowId, Long userId, SheetRowRequest r) {
        verify(documentId, userId);
        SheetRow row = row(rowId, documentId);
        row.updateRowData(json(r.rowData()));
        return response(row);
    }

    @Transactional
    public void deleteRow(Long documentId, Long rowId, Long userId) {
        verify(documentId, userId);
        rowRepository.delete(row(rowId, documentId));
    }

    private void verify(Long id, Long user) {
        Document d = documentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        if (!d.getOwnerUserId().equals(user)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        if (d.getWritingFormat() != com.autodoc.domain.template.WritingFormat.SHEETS)
            throw new BusinessException(ErrorCode.INVALID_DOCUMENT_FORMAT);
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
