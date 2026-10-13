package com.autodoc.domain.template;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.template.dto.TemplateCreateRequest;
import com.autodoc.domain.template.dto.TemplateResponse;
import com.autodoc.domain.template.dto.TemplateUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.seed.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("local")
@Transactional
@DisplayName("문서 템플릿 서비스")
class DocumentTemplateServiceTest {
    private static final Logger log = LoggerFactory.getLogger(DocumentTemplateServiceTest.class);
    @Autowired private DocumentTemplateService templateService;
    @Autowired private DocumentTemplateRepository templateRepository;

    @Test
    @DisplayName("사용자 템플릿을 생성한다")
    void createsUserTemplate() {
        TemplateResponse response = templateService.createUserTemplate(1L, request("service-create-template"));
        assertThat(response.scope()).isEqualTo(TemplateScope.USER);
        log.info("사용자 템플릿 생성 서비스 검증 완료: templateId={}", response.id());
    }

    @Test
    @DisplayName("사용 가능한 목록에는 기본 템플릿과 본인 템플릿만 포함한다")
    void getsBasicAndOwnTemplatesOnly() {
        templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC, "service-basic-template", WritingFormat.DOCS, "report", "{}", "{}"));
        templateRepository.saveAndFlush(new DocumentTemplate(1L, "service-own-template", WritingFormat.DOCS, "report", "{}", "{}"));
        templateRepository.saveAndFlush(new DocumentTemplate(2L, "service-other-template", WritingFormat.DOCS, "report", "{}", "{}"));
        assertThat(templateService.getAvailableTemplates(1L)).extracting(TemplateResponse::name)
                .contains("service-basic-template", "service-own-template").doesNotContain("service-other-template");
        log.info("사용 가능 템플릿 목록 조회 서비스 검증 완료");
    }

    @Test
    @DisplayName("접근 가능한 템플릿을 상세 조회한다")
    void getsAccessibleTemplate() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(1L, "service-get-template", WritingFormat.DOCS, "report", "{}", "{}"));
        assertThat(templateService.getTemplate(template.getId(), 1L).name()).isEqualTo("service-get-template");
        log.info("템플릿 상세 조회 서비스 검증 완료: templateId={}", template.getId());
    }

    @Test
    @DisplayName("다른 회원의 사용자 템플릿에 접근할 수 없다")
    void rejectsOtherUsersTemplate() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(2L, "service-denied-template", WritingFormat.DOCS, "report", "{}", "{}"));
        assertBusinessException(ErrorCode.ACCESS_DENIED, () -> templateService.getTemplate(template.getId(), 1L));
        log.info("타 사용자 템플릿 접근 권한 검증 완료");
    }

    @Test
    @DisplayName("본인의 사용자 템플릿을 수정한다")
    void updatesOwnUserTemplate() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(1L, "service-update-template", WritingFormat.DOCS, "report", "{}", "{}"));
        TemplateResponse response = templateService.updateUserTemplate(template.getId(), 1L,
                new TemplateUpdateRequest("service-updated-template", "handover", "{\"body\":true}", "{\"fontSize\":14}"));
        assertThat(response.name()).isEqualTo("service-updated-template");
        log.info("사용자 템플릿 수정 서비스 검증 완료: templateId={}", template.getId());
    }

    @Test
    @DisplayName("사용자는 기본 템플릿을 수정할 수 없다")
    void rejectsBasicTemplateUpdate() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(TemplateScope.BASIC, "service-basic-update", WritingFormat.DOCS, "report", "{}", "{}"));
        assertBusinessException(ErrorCode.ACCESS_DENIED, () -> templateService.updateUserTemplate(template.getId(), 1L,
                new TemplateUpdateRequest("updated", "report", "{}", "{}")));
        log.info("기본 템플릿 수정 권한 검증 완료");
    }

    @Test
    @DisplayName("본인의 사용자 템플릿을 삭제한다")
    void deletesOwnUserTemplate() {
        DocumentTemplate template = templateRepository.saveAndFlush(new DocumentTemplate(1L, "service-delete-template", WritingFormat.DOCS, "report", "{}", "{}"));
        templateService.deleteUserTemplate(template.getId(), 1L);
        assertThat(templateRepository.findById(template.getId())).isEmpty();
        log.info("사용자 템플릿 삭제 서비스 검증 완료: templateId={}", template.getId());
    }

    private TemplateCreateRequest request(String name) {
        return new TemplateCreateRequest(name, WritingFormat.DOCS, "report", "{\"sections\":[\"title\"]}", "{\"fontSize\":12}");
    }

    private void assertBusinessException(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
