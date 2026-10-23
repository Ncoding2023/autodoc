package com.autodoc.domain.template;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "template_fields", uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_fields_template_key", columnNames = {"template_id", "field_key"}),
        @UniqueConstraint(name = "uk_template_fields_template_order", columnNames = {"template_id", "field_order"})})
public class TemplateField {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_field_id") private Long id;
    @Column(name = "template_id", nullable = false, updatable = false) private Long templateId;
    @Column(name = "field_key", nullable = false, updatable = false, length = 100) private String fieldKey;
    @Column(name = "field_label", nullable = false, length = 100) private String label;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20) private TemplateFieldType fieldType;
    @Column(name = "field_order", nullable = false, updatable = false) private Integer fieldOrder;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected TemplateField() {}

    public TemplateField(Long templateId, String fieldKey, String label, TemplateFieldType fieldType, Integer fieldOrder) {
        this.templateId = templateId;
        this.fieldKey = fieldKey;
        this.label = label;
        this.fieldType = fieldType;
        this.fieldOrder = fieldOrder;
    }

    public Long getId() { return id; }
    public Long getTemplateId() { return templateId; }
    public String getFieldKey() { return fieldKey; }
    public String getLabel() { return label; }
    public TemplateFieldType getFieldType() { return fieldType; }
    public Integer getFieldOrder() { return fieldOrder; }

    public void updateSettings(String label, TemplateFieldType fieldType) {
        this.label = label;
        this.fieldType = fieldType;
    }

    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
