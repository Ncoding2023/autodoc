package com.autodoc.domain.user;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.team.Team;
import com.autodoc.domain.team.TeamRepository;
import com.autodoc.domain.user.dto.UserCreateRequest;
import com.autodoc.domain.user.dto.UserResponse;
import com.autodoc.domain.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.seed.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("local")
@Transactional
class UserServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void signsUpUserWithEncodedPassword() {
        UserResponse response = userService.signUp(new UserCreateRequest(null, "service-signup@example.test", "password1234", "회원가입 사용자"));

        User saved = userRepository.findById(response.id()).orElseThrow();
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("password1234", saved.getPassword())).isTrue();
        log.info("회원가입 서비스 검증 완료: userId={}", response.id());
    }

    @Test
    void signsUpUserWithTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-signup-team", "회원가입 테스트 팀"));

        UserResponse response = userService.signUp(new UserCreateRequest(team.getId(), "service-team-user@example.test", "password1234", "팀 소속 사용자"));

        assertThat(response.teamId()).isEqualTo(team.getId());
        log.info("팀 소속 회원가입 서비스 검증 완료: userId={}, teamId={}", response.id(), team.getId());
    }

    @Test
    void rejectsDuplicateEmailOnSignUp() {
        userRepository.saveAndFlush(new User(null, "service-duplicate@example.test", "encoded-password", "기존 사용자", UserRole.USER, UserStatus.ACTIVE));

        assertBusinessException(ErrorCode.DUPLICATE_EMAIL,
                () -> userService.signUp(new UserCreateRequest(null, "service-duplicate@example.test", "password1234", "중복 사용자")));
        log.info("회원가입 이메일 중복 검증 완료");
    }

    @Test
    void rejectsUnknownTeamOnSignUp() {
        assertBusinessException(ErrorCode.TEAM_NOT_FOUND,
                () -> userService.signUp(new UserCreateRequest(999999L, "service-unknown-team@example.test", "password1234", "팀 오류 사용자")));
        log.info("회원가입 소속 팀 존재 여부 검증 완료");
    }

    @Test
    void getsUser() {
        User user = userRepository.saveAndFlush(new User(null, "service-get@example.test", "encoded-password", "조회 사용자", UserRole.USER, UserStatus.ACTIVE));

        UserResponse response = userService.getUser(user.getId());

        assertThat(response.email()).isEqualTo("service-get@example.test");
        log.info("회원 조회 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    void rejectsUnknownUserOnGet() {
        assertBusinessException(ErrorCode.USER_NOT_FOUND, () -> userService.getUser(999999L));
        log.info("회원 조회 존재 여부 검증 완료");
    }

    @Test
    void updatesUserNameAndTeam() {
        User user = userRepository.saveAndFlush(new User(null, "service-update@example.test", "encoded-password", "수정 전 사용자", UserRole.USER, UserStatus.ACTIVE));
        Team team = teamRepository.saveAndFlush(new Team("service-update-team", "회원 수정 테스트 팀"));

        UserResponse response = userService.updateUser(user.getId(), new UserUpdateRequest(team.getId(), "수정 후 사용자"));

        assertThat(response.name()).isEqualTo("수정 후 사용자");
        assertThat(response.teamId()).isEqualTo(team.getId());
        log.info("회원 이름 및 소속 팀 수정 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    void clearsUserTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-clear-team", "소속 해제 테스트 팀"));
        User user = userRepository.saveAndFlush(new User(team.getId(), "service-clear-team@example.test", "encoded-password", "소속 해제 사용자", UserRole.USER, UserStatus.ACTIVE));

        UserResponse response = userService.updateUser(user.getId(), new UserUpdateRequest(null, "소속 해제 사용자"));

        assertThat(response.teamId()).isNull();
        log.info("회원 소속 팀 해제 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    void rejectsUnknownTeamOnUpdate() {
        User user = userRepository.saveAndFlush(new User(null, "service-update-team-error@example.test", "encoded-password", "팀 오류 사용자", UserRole.USER, UserStatus.ACTIVE));

        assertBusinessException(ErrorCode.TEAM_NOT_FOUND,
                () -> userService.updateUser(user.getId(), new UserUpdateRequest(999999L, "수정 사용자")));
        log.info("회원 수정 소속 팀 존재 여부 검증 완료: userId={}", user.getId());
    }

    @Test
    void rejectsUnknownUserOnUpdate() {
        assertBusinessException(ErrorCode.USER_NOT_FOUND,
                () -> userService.updateUser(999999L, new UserUpdateRequest(null, "수정 사용자")));
        log.info("회원 수정 대상 존재 여부 검증 완료");
    }

    private void assertBusinessException(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
