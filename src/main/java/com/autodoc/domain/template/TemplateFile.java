package com.autodoc.domain.template;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "template_files", uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_files_template_id", columnNames = "template_id"),
        @UniqueConstraint(name = "uk_template_files_stored_name", columnNames = "stored_name")})
public class TemplateFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "template_file_id") private Long id;
    @Column(name = "template_id", nullable = false) private Long templateId;
    @Column(name = "owner_user_id", nullable = false) private Long ownerUserId;
    @Column(name = "original_name", nullable = false, length = 255) private String originalName;
    @Column(name = "stored_name", nullable = false, length = 255) private String storedName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TemplateFileExtension extension;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(name = "storage_path", nullable = false, length = 500) private String storagePath;
    @Column(name = "file_size", nullable = false) private Long fileSize;
    @Column(nullable = false, length = 128) private String checksum;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    protected TemplateFile() {}
    public TemplateFile(Long templateId, Long ownerUserId, String originalName, String storedName,
                        TemplateFileExtension extension, String mimeType, String storagePath, Long fileSize, String checksum) {
        this.templateId = templateId;
        this.ownerUserId = ownerUserId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }
    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public void updateOriginalName(String originalName) { this.originalName = originalName; }
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
