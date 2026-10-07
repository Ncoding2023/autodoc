package com.autodoc.domain.sheet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SheetRowRepository extends JpaRepository<SheetRow, Long> {
    void deleteByDocumentId(Long documentId);
    List<SheetRow> findByDocumentIdOrderByRowNo(Long documentId);
}
