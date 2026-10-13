package com.autodoc.domain.user;

import com.autodoc.domain.user.dto.UserCreateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회원가입 비밀번호 입력 검증")
class UserCreateRequestValidationTest {
    private static final Logger log = LoggerFactory.getLogger(UserCreateRequestValidationTest.class);
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("8자 이상이며 특수문자를 포함한 비밀번호를 허용한다")
    void acceptsPasswordWithSpecialCharacter() {
        assertThat(validator.validate(request("1234567!"))).isEmpty();
        assertThat(validator.validate(request("비밀번호입력입니다!"))).isEmpty();
        log.info("8자 이상 및 특수문자 포함 비밀번호 허용 검증 완료");
    }

    @Test
    @DisplayName("특수문자가 없는 비밀번호는 거부한다")
    void rejectsPasswordWithoutSpecialCharacter() {
        assertThat(validator.validate(request("password1234")))
                .anyMatch(violation -> violation.getMessage().equals("비밀번호는 특수문자를 하나 이상 포함해야 합니다."));
        log.info("특수문자 미포함 비밀번호 거부 검증 완료");
    }

    @Test
    @DisplayName("공백과 한글은 특수문자로 인정하지 않는다")
    void doesNotCountWhitespaceOrLettersAsSpecialCharacters() {
        assertThat(validator.validate(request("password 1234"))).isNotEmpty();
        assertThat(validator.validate(request("비밀번호입력입니다"))).isNotEmpty();
        log.info("공백 및 한글의 특수문자 대체 불가 검증 완료");
    }

    @Test
    @DisplayName("특수문자가 있어도 8자 미만의 비밀번호는 거부한다")
    void rejectsShortPassword() {
        assertThat(validator.validate(request("123456!"))).isNotEmpty();
        log.info("8자 미만 비밀번호 거부 검증 완료");
    }

    @Test
    @DisplayName("비밀번호가 없거나 공백으로만 구성되면 거부한다")
    void rejectsMissingOrBlankPassword() {
        assertThat(validator.validate(request(null))).isNotEmpty();
        assertThat(validator.validate(request(""))).isNotEmpty();
        assertThat(validator.validate(request("        "))).isNotEmpty();
        log.info("비밀번호 누락 및 공백 입력 거부 검증 완료");
    }

    @Test
    @DisplayName("UTF-8 기준 72바이트 경계는 허용하고 초과 입력은 거부한다")
    void checksBcryptByteLimit() {
        assertThat(validator.validate(request("a".repeat(71) + "!"))).isEmpty();
        assertThat(validator.validate(request("가".repeat(23) + "ab!"))).isEmpty();
        assertThat(validator.validate(request("a".repeat(72) + "!"))).isNotEmpty();
        assertThat(validator.validate(request("가".repeat(24) + "!")))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("passwordWithinByteLimit"));
        log.info("UTF-8 기준 72바이트 비밀번호 경계 검증 완료");
    }

    private UserCreateRequest request(String password) {
        return new UserCreateRequest(null, "validation@example.test", password, "검증 사용자");
    }
}
