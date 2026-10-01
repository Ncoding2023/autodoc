package com.autodoc.domain.document;

import com.autodoc.domain.template.WritingFormat;
import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class DocumentRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(DocumentRepositoryTest.class);
    @Autowired private DocumentRepository documentRepository;

    @Test void createsDocument() {
        Document saved = documentRepository.saveAndFlush(document("create"));
        assertThat(saved.getId()).isNotNull();
        log.info("문서 생성 완료: id={}", saved.getId());
    }

    @Test void readsDocument() {
        Document saved = documentRepository.saveAndFlush(document("read"));
        assertThat(documentRepository.findById(saved.getId())).get().extracting(Document::getTitle).isEqualTo("read");
        log.info("문서 조회 완료: id={}", saved.getId());
    }

    @Test void updatesDocument() {
        Document saved = documentRepository.saveAndFlush(document("before"));
        documentRepository.findById(saved.getId()).orElseThrow()
                .updateContent("after", "handover", "updated body", "updated note", "updated caution");
        documentRepository.flush();
        assertThat(documentRepository.findById(saved.getId())).get().extracting(Document::getTitle).isEqualTo("after");
        log.info("문서 수정 완료: id={}", saved.getId());
    }

    @Test void deletesDocument() {
        Document saved = documentRepository.saveAndFlush(document("delete"));
        documentRepository.deleteById(saved.getId());
        documentRepository.flush();
        assertThat(documentRepository.findById(saved.getId())).isEmpty();
        log.info("문서 삭제 완료: id={}", saved.getId());
    }

    private Document document(String title) {
        return new Document(1L, 1L, title, WritingFormat.DOCS, "report", "body", "note", "caution");
    }
}
