package com.autodoc.domain.document;

import com.autodoc.domain.template.*;
import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
@DisplayName("DOCS 항목 데이터 저장")
class DocumentFieldPersistenceTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(DocumentFieldPersistenceTest.class);
    @PersistenceContext private EntityManager entityManager;

    @Test
    @DisplayName("항목명을 바꿔 저장해도 키·입력값·다른 문서와 원본 연결은 유지한다")
    void persistsRenamedLabelWithoutChangingValuesOrOtherDocuments() {
        var template = new DocumentTemplate(TemplateScope.BASIC, "항목 저장 검증", WritingFormat.DOCS, "handover", "{}", "{}");
        entityManager.persist(template);
        var file = new TemplateFile(template.getId(), 1L, "인수인계서.docx", "field-test-" + java.util.UUID.randomUUID(),
                TemplateFileExtension.DOCX, "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "/test/양식.docx", 1L, "test-checksum");
        entityManager.persist(file);
        var definition = new TemplateField(template.getId(), "담당자", "담당자", TemplateFieldType.TEXT, 1);
        entityManager.persist(definition);
        var first = new Document(1L, template.getId(), "첫 문서", WritingFormat.DOCS, "handover", null, null, null, file.getId());
        var second = new Document(1L, template.getId(), "두 번째 문서", WritingFormat.DOCS, "handover", null, null, null, file.getId());
        entityManager.persist(first);
        entityManager.persist(second);
        var firstField = new DocumentField(first.getId(), definition);
        firstField.updateValue("김담당");
        var secondField = new DocumentField(second.getId(), definition);
        secondField.updateValue("이담당");
        entityManager.persist(firstField);
        entityManager.persist(secondField);
        entityManager.flush();
        firstField.renameLabel("업무자");
        entityManager.flush();
        entityManager.clear();

        var reloaded = entityManager.find(DocumentField.class, firstField.getId());
        assertThat(reloaded.getLabel()).isEqualTo("업무자");
        assertThat(reloaded.getValue()).isEqualTo("김담당");
        assertThat(reloaded.getFieldKey()).isEqualTo("담당자");
        assertThat(reloaded.getFieldType()).isEqualTo(TemplateFieldType.TEXT);
        assertThat(reloaded.getFieldOrder()).isEqualTo(1);
        assertThat(reloaded.getDocumentId()).isEqualTo(first.getId());
        assertThat(entityManager.find(TemplateField.class, definition.getId()).getLabel()).isEqualTo("담당자");
        assertThat(entityManager.find(DocumentField.class, secondField.getId()).getLabel()).isEqualTo("담당자");
        assertThat(entityManager.find(DocumentField.class, secondField.getId()).getValue()).isEqualTo("이담당");
        assertThat(entityManager.find(Document.class, first.getId()).getSourceTemplateFileId()).isEqualTo(file.getId());
        log.info("항목명 수정 후 키·입력값·문서 간 독립성과 원본 파일 연결 저장 검증 완료");
    }
}
