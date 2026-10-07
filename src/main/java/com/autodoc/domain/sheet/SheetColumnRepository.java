package com.autodoc.domain.sheet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SheetColumnRepository extends JpaRepository<SheetColumn, Long> {
    void deleteByDocumentId(Long documentId);
    List<SheetColumn> findByDocumentIdOrderByColumnOrder(Long documentId);
}
