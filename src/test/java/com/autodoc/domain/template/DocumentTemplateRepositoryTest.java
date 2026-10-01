package com.autodoc.domain.template;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class DocumentTemplateRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(DocumentTemplateRepositoryTest.class);
    @Autowired private DocumentTemplateRepository templateRepository;

    @Test void createsTemplate() {
        DocumentTemplate saved = templateRepository.saveAndFlush(template("repository-create-template"));
        assertThat(saved.getId()).isNotNull();
        log.info("템플릿 생성 완료: id={}", saved.getId());
    }

    @Test void readsTemplateAndChecksScopeAndName() {
        DocumentTemplate saved = templateRepository.saveAndFlush(template("repository-read-template"));
        assertThat(templateRepository.findById(saved.getId())).isPresent();
        assertThat(templateRepository.existsByTemplateScopeAndName(TemplateScope.BASIC, "repository-read-template")).isTrue();
        assertThat(templateRepository.existsByTemplateScopeAndName(TemplateScope.USER, "repository-read-template")).isFalse();
        log.info("템플릿 조회 완료: id={}", saved.getId());
    }

    @Test void updatesTemplate() {
        DocumentTemplate saved = templateRepository.saveAndFlush(template("repository-update-template"));
        DocumentTemplate found = templateRepository.findById(saved.getId()).orElseThrow();
        found.update("repository-updated-template", "handover", "{\"sections\":[\"body\"]}", "{\"fontSize\":14}");
        templateRepository.flush();
        assertThat(templateRepository.findById(saved.getId())).get().extracting(DocumentTemplate::getName)
                .isEqualTo("repository-updated-template");
        log.info("템플릿 수정 완료: id={}", saved.getId());
    }

    @Test void deletesTemplate() {
        DocumentTemplate saved = templateRepository.saveAndFlush(template("repository-delete-template"));
        templateRepository.deleteById(saved.getId());
        templateRepository.flush();
        assertThat(templateRepository.findById(saved.getId())).isEmpty();
        log.info("템플릿 삭제 완료: id={}", saved.getId());
    }

    private DocumentTemplate template(String name) {
        return new DocumentTemplate(TemplateScope.BASIC, name, WritingFormat.DOCS, "report",
                "{\"sections\":[\"title\"]}", "{\"fontSize\":12}");
    }
}
