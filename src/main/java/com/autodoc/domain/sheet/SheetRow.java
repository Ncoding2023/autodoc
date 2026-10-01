package com.autodoc.domain.sheet;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "sheet_rows", uniqueConstraints = @UniqueConstraint(name = "uk_sheet_rows_document_row_no", columnNames = {"document_id", "row_no"}))
public class SheetRow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "sheet_row_id") private Long id;
    @Column(name = "document_id", nullable = false) private Long documentId;
    @Column(name = "row_no", nullable = false) private Integer rowNo;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "row_data", nullable = false, columnDefinition = "jsonb") private String rowData;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected SheetRow() {}
    public SheetRow(Long documentId, Integer rowNo, String rowData) {
        this.documentId = documentId;
        this.rowNo = rowNo;
        this.rowData = rowData;
    }
    public Long getId() { return id; }
    public String getRowData() { return rowData; }
    public void updateRowData(String rowData) { this.rowData = rowData; }
    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
