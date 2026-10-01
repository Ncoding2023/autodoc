package com.autodoc.domain.template;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class TemplateFileRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(TemplateFileRepositoryTest.class);
    @Autowired private TemplateFileRepository templateFileRepository;

    @Test void createsTemplateFile() {
        TemplateFile saved = templateFileRepository.saveAndFlush(file("create.docx", "create-template"));
        assertThat(saved.getId()).isNotNull();
        log.info("템플릿 파일 생성 완료: id={}", saved.getId());
    }

    @Test void readsTemplateFile() {
        TemplateFile saved = templateFileRepository.saveAndFlush(file("read.docx", "read-template"));
        assertThat(templateFileRepository.findById(saved.getId())).get().extracting(TemplateFile::getOriginalName).isEqualTo("read.docx");
        log.info("템플릿 파일 조회 완료: id={}", saved.getId());
    }

    @Test void updatesTemplateFile() {
        TemplateFile saved = templateFileRepository.saveAndFlush(file("before.docx", "update-template"));
        templateFileRepository.findById(saved.getId()).orElseThrow().updateOriginalName("after.docx");
        templateFileRepository.flush();
        assertThat(templateFileRepository.findById(saved.getId())).get().extracting(TemplateFile::getOriginalName).isEqualTo("after.docx");
        log.info("템플릿 파일 수정 완료: id={}", saved.getId());
    }

    @Test void deletesTemplateFile() {
        TemplateFile saved = templateFileRepository.saveAndFlush(file("delete.docx", "delete-template"));
        templateFileRepository.deleteById(saved.getId());
        templateFileRepository.flush();
        assertThat(templateFileRepository.findById(saved.getId())).isEmpty();
        log.info("템플릿 파일 삭제 완료: id={}", saved.getId());
    }

    private TemplateFile file(String originalName, String storedName) {
        return new TemplateFile(1L, 1L, originalName, storedName, TemplateFileExtension.DOCX,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "/templates/" + storedName, 10L, "checksum");
    }
}
