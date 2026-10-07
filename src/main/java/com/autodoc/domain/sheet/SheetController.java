package com.autodoc.domain.sheet;

import com.autodoc.domain.auth.AuthService;
import com.autodoc.domain.sheet.dto.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/documents/{documentId}")
public class SheetController {
    private final SheetService service;
    private final AuthService auth;

    public SheetController(SheetService s, AuthService a) {
        service = s;
        auth = a;
    }

    private Long user(HttpSession s) {
        return auth.getCurrentUser(s).id();
    }

    @PostMapping("/columns")
    public ResponseEntity<SheetColumnResponse> addColumn(@PathVariable Long documentId, @Valid @RequestBody SheetColumnCreateRequest r, HttpSession s) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addColumn(documentId, user(s), r));
    }

    @GetMapping("/columns")
    public List<SheetColumnResponse> getColumns(@PathVariable Long documentId, HttpSession s) {
        return service.getColumns(documentId, user(s));
    }

    @PatchMapping("/columns/{columnId}")
    public SheetColumnResponse updateColumn(@PathVariable Long documentId, @PathVariable Long columnId, @Valid @RequestBody SheetColumnUpdateRequest r, HttpSession s) {
        return service.updateColumn(documentId, columnId, user(s), r);
    }

    @PatchMapping("/columns/order")
    public List<SheetColumnResponse> updateColumnOrder(@PathVariable Long documentId, @Valid @RequestBody SheetColumnOrderRequest r, HttpSession s) {
        return service.updateColumnOrder(documentId, user(s), r);
    }

    @DeleteMapping("/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable Long documentId, @PathVariable Long columnId, HttpSession s) {
        service.deleteColumn(documentId, columnId, user(s));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rows")
    public ResponseEntity<SheetRowResponse> addRow(@PathVariable Long documentId, @Valid @RequestBody SheetRowRequest r, HttpSession s) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRow(documentId, user(s), r));
    }

    @GetMapping("/rows")
    public List<SheetRowResponse> getRows(@PathVariable Long documentId, HttpSession s) {
        return service.getRows(documentId, user(s));
    }

    @PatchMapping("/rows/{rowId}")
    public SheetRowResponse updateRow(@PathVariable Long documentId, @PathVariable Long rowId, @Valid @RequestBody SheetRowRequest r, HttpSession s) {
        return service.updateRow(documentId, rowId, user(s), r);
    }

    @DeleteMapping("/rows/{rowId}")
    public ResponseEntity<Void> deleteRow(@PathVariable Long documentId, @PathVariable Long rowId, HttpSession s) {
        service.deleteRow(documentId, rowId, user(s));
        return ResponseEntity.noContent().build();
    }
}
