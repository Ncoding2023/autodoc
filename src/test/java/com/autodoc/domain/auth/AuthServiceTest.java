package com.autodoc.domain.auth;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.auth.dto.LoginRequest;
import com.autodoc.domain.user.User;
import com.autodoc.domain.user.UserRepository;
import com.autodoc.domain.user.UserRole;
import com.autodoc.domain.user.UserStatus;
import com.autodoc.domain.user.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.seed.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("local")
@Transactional
@DisplayName("인증 서비스")
class AuthServiceTest {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceTest.class);

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("로그인 성공 시 회원 ID를 세션에 저장한다")
    void logsInAndStoresUserIdInSession() {
        User user = userRepository.saveAndFlush(user("auth-login@example.test", UserStatus.ACTIVE));
        MockHttpSession session = new MockHttpSession();

        UserResponse response = authService.login(new LoginRequest(user.getEmail(), "password1234"), session);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(session.getAttribute(AuthSession.LOGIN_USER_ID)).isEqualTo(user.getId());
        log.info("로그인 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("등록되지 않은 이메일로 로그인할 수 없다")
    void rejectsUnknownEmailOnLogin() {
        MockHttpSession session = new MockHttpSession();

        assertBusinessException(ErrorCode.INVALID_CREDENTIALS,
                () -> authService.login(new LoginRequest("unknown@example.test", "password1234"), session));
        log.info("로그인 이메일 존재 여부 검증 완료");
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인할 수 없다")
    void rejectsInvalidPasswordOnLogin() {
        User user = userRepository.saveAndFlush(user("auth-password@example.test", UserStatus.ACTIVE));
        MockHttpSession session = new MockHttpSession();

        assertBusinessException(ErrorCode.INVALID_CREDENTIALS,
                () -> authService.login(new LoginRequest(user.getEmail(), "wrong-password"), session));
        log.info("로그인 비밀번호 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("비활성 회원은 로그인할 수 없다")
    void rejectsInactiveUserOnLogin() {
        User user = userRepository.saveAndFlush(user("auth-inactive@example.test", UserStatus.INACTIVE));
        MockHttpSession session = new MockHttpSession();

        assertBusinessException(ErrorCode.INACTIVE_USER,
                () -> authService.login(new LoginRequest(user.getEmail(), "password1234"), session));
        log.info("로그인 회원 상태 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("로그아웃 시 세션을 무효화한다")
    void logsOutByInvalidatingSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER_ID, 1L);

        authService.logout(session);

        assertThat(session.isInvalid()).isTrue();
        log.info("로그아웃 서비스 검증 완료");
    }

    @Test
    @DisplayName("세션에 저장된 현재 로그인 회원을 조회한다")
    void getsCurrentLoggedInUser() {
        User user = userRepository.saveAndFlush(user("auth-me@example.test", UserStatus.ACTIVE));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthSession.LOGIN_USER_ID, user.getId());

        UserResponse response = authService.getCurrentUser(session);

        assertThat(response.email()).isEqualTo(user.getEmail());
        log.info("현재 로그인 사용자 조회 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("로그인 정보가 없는 세션으로 현재 회원을 조회할 수 없다")
    void rejectsMissingSessionOnCurrentUserLookup() {
        assertBusinessException(ErrorCode.UNAUTHORIZED, () -> authService.getCurrentUser(new MockHttpSession()));
        log.info("현재 로그인 사용자 세션 검증 완료");
    }

    private User user(String email, UserStatus status) {
        return new User(null, email, passwordEncoder.encode("password1234"), "인증 테스트 사용자", UserRole.USER, status);
    }

    private void assertBusinessException(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
