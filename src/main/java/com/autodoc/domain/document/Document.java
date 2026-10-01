package com.autodoc.domain.document;

import com.autodoc.domain.template.WritingFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "document_id") private Long id;
    @Column(name = "owner_user_id", nullable = false) private Long ownerUserId;
    @Column(name = "template_id", nullable = false) private Long templateId;
    @Column(nullable = false, length = 255) private String title;
    @Enumerated(EnumType.STRING) @Column(name = "writing_format", nullable = false, length = 10) private WritingFormat writingFormat;
    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @Column(name = "body_content", columnDefinition = "TEXT") private String bodyContent;
    @Column(name = "important_notes", columnDefinition = "TEXT") private String importantNotes;
    @Column(columnDefinition = "TEXT") private String cautions;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DocumentStatus status = DocumentStatus.DRAFT;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected Document() {}
    public Document(Long ownerUserId, Long templateId, String title, WritingFormat writingFormat, String documentType,
                    String bodyContent, String importantNotes, String cautions) {
        this.ownerUserId = ownerUserId;
        this.templateId = templateId;
        this.title = title;
        this.writingFormat = writingFormat;
        this.documentType = documentType;
        this.bodyContent = bodyContent;
        this.importantNotes = importantNotes;
        this.cautions = cautions;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public DocumentStatus getStatus() { return status; }
    public void updateContent(String title, String documentType, String bodyContent, String importantNotes, String cautions) {
        this.title = title;
        this.documentType = documentType;
        this.bodyContent = bodyContent;
        this.importantNotes = importantNotes;
        this.cautions = cautions;
    }
    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
