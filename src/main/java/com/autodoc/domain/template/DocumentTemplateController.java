package com.autodoc.domain.template;

import com.autodoc.domain.auth.AuthService;
import com.autodoc.domain.template.dto.TemplateCreateRequest;
import com.autodoc.domain.template.dto.TemplateResponse;
import com.autodoc.domain.template.dto.TemplateUpdateRequest;
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
@RequestMapping("/api/templates")
public class DocumentTemplateController {
    private final DocumentTemplateService templateService;
    private final AuthService authService;

    public DocumentTemplateController(DocumentTemplateService templateService, AuthService authService) {
        this.templateService = templateService;
        this.authService = authService;
    }

    @GetMapping
    public List<TemplateResponse> getTemplates(HttpSession session) {
        return templateService.getAvailableTemplates(currentUser(session).id());
    }

    @GetMapping("/{templateId}")
    public TemplateResponse getTemplate(@PathVariable Long templateId, HttpSession session) {
        return templateService.getTemplate(templateId, currentUser(session).id());
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody TemplateCreateRequest request, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createUserTemplate(currentUser(session).id(), request));
    }

    @PatchMapping("/{templateId}")
    public TemplateResponse updateTemplate(@PathVariable Long templateId, @Valid @RequestBody TemplateUpdateRequest request, HttpSession session) {
        return templateService.updateUserTemplate(templateId, currentUser(session).id(), request);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId, HttpSession session) {
        templateService.deleteUserTemplate(templateId, currentUser(session).id());
        return ResponseEntity.noContent().build();
    }

    private UserResponse currentUser(HttpSession session) { return authService.getCurrentUser(session); }
}
