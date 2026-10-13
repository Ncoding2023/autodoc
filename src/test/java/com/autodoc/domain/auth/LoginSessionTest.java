package com.autodoc.domain.auth;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.domain.auth.dto.LoginRequest;
import com.autodoc.domain.user.User;
import com.autodoc.domain.user.UserRepository;
import com.autodoc.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("로그인 유지 및 세션 관리")
class LoginSessionTest {
    private static final Logger log = LoggerFactory.getLogger(LoginSessionTest.class);

    private AuthController controller;
    private PasswordEncoder encoder;
    private MockHttpSession session;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        UserRepository repository = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPassword()).thenReturn("encoded");
        when(repository.findByEmail("session@example.test")).thenReturn(Optional.of(user));
        when(encoder.matches("password1234", "encoded")).thenReturn(true);
        controller = new AuthController(new AuthService(repository, encoder));
        request = new MockHttpServletRequest();
        session = new MockHttpSession(request.getServletContext());
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("로그인 유지 선택 시 7일 유지 쿠키를 발급하고 세션 ID를 변경한다")
    void remembersLoginWithPersistentHttpOnlyCookieAndRotatesSessionId() {
        String previousId = session.getId();
        controller.login(new LoginRequest("session@example.test", "password1234", true), session, request, response);
        assertThat(session.getId()).isNotEqualTo(previousId);
        assertThat(session.getAttribute(AuthSession.LOGIN_USER_ID)).isEqualTo(1L);
        assertThat(session.getMaxInactiveInterval()).isEqualTo(604800);
        assertThat(response.getHeader("Set-Cookie")).contains("JSESSIONID=" + session.getId(),
                "Max-Age=604800", "HttpOnly", "SameSite=Lax", "Path=/");
        log.info("로그인 유지 쿠키 발급 및 세션 ID 변경 검증 완료");
    }

    @Test
    @DisplayName("로그인 유지 해제 시 30분 비활동 만료와 일반 세션 쿠키로 전환한다")
    void uncheckedLoginReplacesPreviousPersistentCookie() {
        controller.login(new LoginRequest("session@example.test", "password1234", true), session, request, response);
        controller.login(new LoginRequest("session@example.test", "password1234"), session, request, response);
        assertThat(session.getMaxInactiveInterval()).isEqualTo(1800);
        assertThat(response.getHeader("Set-Cookie")).contains("JSESSIONID=" + session.getId())
                .doesNotContain("Max-Age", "Expires");
        log.info("로그인 유지 해제 및 일반 세션 쿠키 전환 검증 완료");
    }

    @Test
    @DisplayName("로그아웃 시 세션을 무효화하고 로그인 쿠키를 삭제한다")
    void logoutInvalidatesSessionAndDeletesCookie() {
        controller.login(new LoginRequest("session@example.test", "password1234", true), session, request, response);
        controller.logout(session, request, response);
        assertThat(session.isInvalid()).isTrue();
        assertThat(response.getHeader("Set-Cookie")).contains("JSESSIONID=;", "Max-Age=0", "Path=/");
        log.info("로그아웃 세션 무효화 및 쿠키 삭제 검증 완료");
    }

    @Test
    @DisplayName("HTTPS 환경에서는 보안 쿠키와 애플리케이션 경로를 적용한다")
    void usesSecureCookieAndApplicationPathOnHttps() {
        request.setSecure(true);
        request.setContextPath("/autodoc");
        controller.login(new LoginRequest("session@example.test", "password1234", true), session, request, response);
        assertThat(response.getHeader("Set-Cookie")).contains("Secure", "Path=/autodoc");
        log.info("HTTPS 보안 쿠키 및 애플리케이션 경로 적용 검증 완료");
    }

    @Test
    @DisplayName("로그인 실패 시 로그인 유지 쿠키를 발급하지 않는다")
    void failedLoginDoesNotEnableRememberMe() {
        when(encoder.matches("password1234", "encoded")).thenReturn(false);
        String previousId = session.getId();
        assertThatThrownBy(() -> controller.login(new LoginRequest("session@example.test", "password1234", true),
                session, request, response)).isInstanceOf(BusinessException.class);
        assertThat(session.getAttribute(AuthSession.LOGIN_USER_ID)).isNull();
        assertThat(session.getId()).isEqualTo(previousId);
        assertThat(response.getHeader("Set-Cookie")).isNull();
        log.info("로그인 실패 시 로그인 유지 쿠키 미발급 검증 완료");
    }
}
