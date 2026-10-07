package com.autodoc.domain.document;

import com.autodoc.domain.auth.AuthService;
import com.autodoc.domain.document.dto.DocumentCreateRequest;
import com.autodoc.domain.document.dto.DocumentListResponse;
import com.autodoc.domain.document.dto.DocumentResponse;
import com.autodoc.domain.document.dto.DocumentUpdateRequest;
import com.autodoc.domain.user.dto.UserResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final AuthService authService;

    public DocumentController(DocumentService documentService, AuthService authService) {
        this.documentService = documentService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentCreateRequest request, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(currentUser(session).id(), request));
    }

    @GetMapping
    public List<DocumentListResponse> getMyDocuments(HttpSession session) {
        return documentService.getMyDocuments(currentUser(session).id());
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@PathVariable Long documentId, HttpSession session) {
        return documentService.getDocument(documentId, currentUser(session).id());
    }

    @PatchMapping("/{documentId}")
    public DocumentResponse updateDocument(@PathVariable Long documentId, @Valid @RequestBody DocumentUpdateRequest request, HttpSession session) {
        return documentService.updateDocument(documentId, currentUser(session).id(), request);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId, HttpSession session) {
        documentService.deleteDocument(documentId, currentUser(session).id());
        return ResponseEntity.noContent().build();
    }

    private UserResponse currentUser(HttpSession session) { return authService.getCurrentUser(session); }
}
