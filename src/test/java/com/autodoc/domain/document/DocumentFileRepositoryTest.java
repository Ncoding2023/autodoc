package com.autodoc.domain.document;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class DocumentFileRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(DocumentFileRepositoryTest.class);
    @Autowired private DocumentFileRepository documentFileRepository;

    @Test void createsDocumentFile() {
        DocumentFile saved = documentFileRepository.saveAndFlush(file("create.txt", "create-stored"));
        assertThat(saved.getId()).isNotNull();
        log.info("문서 파일 생성 완료: id={}", saved.getId());
    }

    @Test void readsDocumentFile() {
        DocumentFile saved = documentFileRepository.saveAndFlush(file("read.txt", "read-stored"));
        assertThat(documentFileRepository.findById(saved.getId())).get().extracting(DocumentFile::getOriginalName).isEqualTo("read.txt");
        log.info("문서 파일 조회 완료: id={}", saved.getId());
    }

    @Test void updatesDocumentFile() {
        DocumentFile saved = documentFileRepository.saveAndFlush(file("before.txt", "update-stored"));
        documentFileRepository.findById(saved.getId()).orElseThrow().updateOriginalName("after.txt");
        documentFileRepository.flush();
        assertThat(documentFileRepository.findById(saved.getId())).get().extracting(DocumentFile::getOriginalName).isEqualTo("after.txt");
        log.info("문서 파일 수정 완료: id={}", saved.getId());
    }

    @Test void deletesDocumentFile() {
        DocumentFile saved = documentFileRepository.saveAndFlush(file("delete.txt", "delete-stored"));
        documentFileRepository.deleteById(saved.getId());
        documentFileRepository.flush();
        assertThat(documentFileRepository.findById(saved.getId())).isEmpty();
        log.info("문서 파일 삭제 완료: id={}", saved.getId());
    }

    private DocumentFile file(String originalName, String storedName) {
        return new DocumentFile(1L, 1L, DocumentFileRole.ATTACHMENT, originalName, storedName, "txt", "text/plain", "/files/" + storedName, 10L, "checksum");
    }
}
