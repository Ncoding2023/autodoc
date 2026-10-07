package com.autodoc.domain.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_files", uniqueConstraints = @UniqueConstraint(name = "uk_document_files_stored_name", columnNames = "stored_name"))
public class DocumentFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "file_id") private Long id;
    @Column(name = "document_id", nullable = false) private Long documentId;
    @Column(name = "owner_user_id", nullable = false) private Long ownerUserId;
    @Enumerated(EnumType.STRING) @Column(name = "file_role", nullable = false, length = 20) private DocumentFileRole fileRole;
    @Column(name = "original_name", nullable = false, length = 255) private String originalName;
    @Column(name = "stored_name", nullable = false, length = 255) private String storedName;
    @Column(nullable = false, length = 10) private String extension;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(name = "storage_path", nullable = false, length = 500) private String storagePath;
    @Column(name = "file_size", nullable = false) private Long fileSize;
    @Column(length = 128) private String checksum;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    protected DocumentFile() {}
    public DocumentFile(Long documentId, Long ownerUserId, DocumentFileRole fileRole, String originalName,
                        String storedName, String extension, String mimeType, String storagePath, Long fileSize, String checksum) {
        this.documentId = documentId;
        this.ownerUserId = ownerUserId;
        this.fileRole = fileRole;
        this.originalName = originalName;
        this.storedName = storedName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }
    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public String getOriginalName() { return originalName; }
    public void updateOriginalName(String originalName) { this.originalName = originalName; }
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
