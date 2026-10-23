package com.autodoc.domain.document;

import com.autodoc.domain.template.TemplateField;
import com.autodoc.domain.template.TemplateFieldType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_fields", uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_fields_document_key", columnNames = {"document_id", "field_key"}),
        @UniqueConstraint(name = "uk_document_fields_document_order", columnNames = {"document_id", "field_order"})})
public class DocumentField {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_field_id") private Long id;
    @Column(name = "document_id", nullable = false, updatable = false) private Long documentId;
    @Column(name = "field_key", nullable = false, updatable = false, length = 100) private String fieldKey;
    @Column(name = "field_label", nullable = false, length = 100) private String label;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, updatable = false, length = 20) private TemplateFieldType fieldType;
    @Column(name = "field_order", nullable = false, updatable = false) private Integer fieldOrder;
    @Column(name = "field_value", columnDefinition = "TEXT") private String value;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected DocumentField() {}

    public DocumentField(Long documentId, TemplateField definition) {
        this.documentId = documentId;
        this.fieldKey = definition.getFieldKey();
        this.label = definition.getLabel();
        this.fieldType = definition.getFieldType();
        this.fieldOrder = definition.getFieldOrder();
    }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public String getFieldKey() { return fieldKey; }
    public String getLabel() { return label; }
    public TemplateFieldType getFieldType() { return fieldType; }
    public Integer getFieldOrder() { return fieldOrder; }
    public String getValue() { return value; }

    public void renameLabel(String label) { this.label = label; }
    public void updateValue(String value) { this.value = value; }

    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
