package com.autodoc.domain.template;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    boolean existsByTemplateScopeAndName(TemplateScope templateScope, String name);
}
