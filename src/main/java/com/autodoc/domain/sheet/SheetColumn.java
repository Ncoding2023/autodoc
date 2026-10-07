package com.autodoc.domain.sheet;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sheet_columns", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sheet_columns_document_key", columnNames = {"document_id", "column_key"}),
        @UniqueConstraint(name = "uk_sheet_columns_document_order", columnNames = {"document_id", "column_order"})})
public class SheetColumn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "sheet_column_id") private Long id;
    @Column(name = "document_id", nullable = false) private Long documentId;
    @Column(name = "column_key", nullable = false, length = 100) private String columnKey;
    @Column(name = "column_name", nullable = false, length = 100) private String columnName;
    @Enumerated(EnumType.STRING) @Column(name = "column_type", nullable = false, length = 20) private SheetColumnType columnType = SheetColumnType.TEXT;
    @Column(name = "column_order", nullable = false) private Integer columnOrder;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected SheetColumn() {}
    public SheetColumn(Long documentId, String columnKey, String columnName, SheetColumnType columnType, Integer columnOrder) {
        this.documentId = documentId;
        this.columnKey = columnKey;
        this.columnName = columnName;
        this.columnType = columnType;
        this.columnOrder = columnOrder;
    }
    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public String getColumnKey() { return columnKey; }
    public String getColumnName() { return columnName; }
    public SheetColumnType getColumnType() { return columnType; }
    public Integer getColumnOrder() { return columnOrder; }
    public void update(String columnName, SheetColumnType columnType) {
        this.columnName = columnName;
        this.columnType = columnType;
    }
    public void updateOrder(Integer columnOrder) { this.columnOrder = columnOrder; }
    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
