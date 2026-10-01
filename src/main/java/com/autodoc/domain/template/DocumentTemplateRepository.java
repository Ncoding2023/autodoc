package com.autodoc.domain.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    boolean existsByTemplateScopeAndName(TemplateScope templateScope, String name);
    List<DocumentTemplate> findByTemplateScopeOrOwnerUserId(TemplateScope templateScope, Long ownerUserId);
}
