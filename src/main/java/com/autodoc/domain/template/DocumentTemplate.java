package com.autodoc.domain.template;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_templates")
public class DocumentTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "template_id") private Long id;
    @Column(name = "owner_user_id") private Long ownerUserId;
    @Enumerated(EnumType.STRING) @Column(name = "template_scope", nullable = false, length = 10) private TemplateScope templateScope;
    @Column(name = "template_name", nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "writing_format", nullable = false, length = 10) private WritingFormat writingFormat;
    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "structure_data", nullable = false, columnDefinition = "jsonb") private String structureData;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "template_style_data", nullable = false, columnDefinition = "jsonb") private String templateStyleData;
    @Enumerated(EnumType.STRING) @Column(name = "analysis_status", nullable = false, length = 10) private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected DocumentTemplate() {}
    public DocumentTemplate(TemplateScope templateScope, String name, WritingFormat writingFormat,
                            String documentType, String structureData, String templateStyleData) {
        this.templateScope = templateScope;
        this.name = name;
        this.writingFormat = writingFormat;
        this.documentType = documentType;
        this.structureData = structureData;
        this.templateStyleData = templateStyleData;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public WritingFormat getWritingFormat() { return writingFormat; }
    public void update(String name, String documentType, String structureData, String templateStyleData) {
        this.name = name;
        this.documentType = documentType;
        this.structureData = structureData;
        this.templateStyleData = templateStyleData;
    }
    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
