package com.autodoc.domain.user;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.team.Team;
import com.autodoc.domain.team.TeamRepository;
import com.autodoc.domain.team.dto.TeamCreateRequest;
import jakarta.validation.Validator;
import org.springframework.transaction.annotation.Propagation;
import com.autodoc.domain.user.dto.UserCreateRequest;
import com.autodoc.domain.user.dto.UserResponse;
import com.autodoc.domain.user.dto.UserUpdateRequest;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("회원 서비스")
class UserServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Validator validator;

    @Test
    @DisplayName("새 팀과 회원을 함께 생성하여 가입한다")
    void signsUpUserAndCreatesTeamTogether() {
        UserResponse response = userService.signUp(new UserCreateRequest(null,
                "signup-new-team@example.test", "password1234!", "새 팀 사용자",
                new TeamCreateRequest("회원가입 신규 팀", "팀 소개")));

        Team team = teamRepository.findById(response.teamId()).orElseThrow();
        assertThat(team.getName()).isEqualTo("회원가입 신규 팀");
        assertThat(team.getDescription()).isEqualTo("팀 소개");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        log.info("새 팀 등록과 회원가입 동시 처리 검증 완료");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("회원 저장에 실패하면 새 팀 생성도 롤백한다")
    void rollsBackNewTeamWhenUserCreationFails() {
        assertThatThrownBy(() -> userService.signUp(new UserCreateRequest(null,
                "signup-rollback@example.test", null, "실패 사용자",
                new TeamCreateRequest("회원가입 롤백 검증 팀", null))))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        assertThat(teamRepository.existsByName("회원가입 롤백 검증 팀")).isFalse();
        assertThat(userRepository.existsByEmail("signup-rollback@example.test")).isFalse();
        log.info("회원가입 실패 시 신규 팀 저장 롤백 검증 완료");
    }

    @Test
    @DisplayName("회원가입 시 새 팀 이름이 중복되면 가입을 거부한다")
    void rejectsDuplicateNewTeamName() {
        teamRepository.saveAndFlush(new Team("회원가입 중복 팀", null));
        assertBusinessException(ErrorCode.DUPLICATE_TEAM_NAME,
                () -> userService.signUp(new UserCreateRequest(null,
                        "signup-duplicate-team@example.test", "password1234!", "팀 사용자",
                        new TeamCreateRequest("회원가입 중복 팀", null))));
        assertThat(userRepository.existsByEmail("signup-duplicate-team@example.test")).isFalse();
        log.info("회원가입 신규 팀 이름 중복 검증 완료");
    }

    @Test
    @DisplayName("기존 팀 선택과 새 팀 등록을 동시에 요청할 수 없다")
    void rejectsExistingAndNewTeamTogether() {
        UserCreateRequest request = new UserCreateRequest(1L, "signup-both@example.test",
                "password1234!", "팀 사용자", new TeamCreateRequest("신규 팀", null));
        assertThat(validator.validate(request)).isNotEmpty();
        assertBusinessException(ErrorCode.INVALID_REQUEST, () -> userService.signUp(request));
        log.info("기존 팀 선택과 새 팀 등록 동시 요청 거부 검증 완료");
    }

    @Test
    @DisplayName("새 팀 이름의 공백과 설명의 최대 길이를 검증한다")
    void validatesNewTeamInput() {
        UserCreateRequest request = new UserCreateRequest(null, "signup-validation@example.test",
                "password1234!", "팀 사용자", new TeamCreateRequest(" ", "x".repeat(256)));
        assertThat(validator.validate(request)).extracting(violation -> violation.getPropertyPath().toString())
                .contains("team.name", "team.description");
        log.info("회원가입 신규 팀 입력값 검증 완료");
    }

    @Test
    @DisplayName("회원가입용 팀 목록에서 팀 ID와 이름을 조회한다")
    void listsTeamsForSignup() {
        Team team = teamRepository.saveAndFlush(new Team("가입 목록 팀", "내부 설명"));
        assertThat(userService.getSignupTeams()).anySatisfy(option -> {
            assertThat(option.id()).isEqualTo(team.getId());
            assertThat(option.name()).isEqualTo(team.getName());
        });
        log.info("회원가입용 팀 선택 목록 조회 검증 완료");
    }

    @Test
    @DisplayName("개인 회원가입 시 비밀번호를 암호화하고 기본 권한과 상태를 설정한다")
    void signsUpUserWithEncodedPassword() {
        UserResponse response = userService.signUp(new UserCreateRequest(null, "service-signup@example.test", "password1234!", "회원가입 사용자"));

        User saved = userRepository.findById(response.id()).orElseThrow();
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("password1234!", saved.getPassword())).isTrue();
        log.info("회원가입 서비스 검증 완료: userId={}", response.id());
    }

    @Test
    @DisplayName("기존 팀에 소속된 회원으로 가입한다")
    void signsUpUserWithTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-signup-team", "회원가입 테스트 팀"));

        UserResponse response = userService.signUp(new UserCreateRequest(team.getId(), "service-team-user@example.test", "password1234!", "팀 소속 사용자"));

        assertThat(response.teamId()).isEqualTo(team.getId());
        log.info("팀 소속 회원가입 서비스 검증 완료: userId={}, teamId={}", response.id(), team.getId());
    }

    @Test
    @DisplayName("중복 이메일로 회원가입할 수 없다")
    void rejectsDuplicateEmailOnSignUp() {
        userRepository.saveAndFlush(new User(null, "service-duplicate@example.test", "encoded-password", "기존 사용자", UserRole.USER, UserStatus.ACTIVE));

        assertBusinessException(ErrorCode.DUPLICATE_EMAIL,
                () -> userService.signUp(new UserCreateRequest(null, "service-duplicate@example.test", "password1234!", "중복 사용자")));
        log.info("회원가입 이메일 중복 검증 완료");
    }

    @Test
    @DisplayName("존재하지 않는 팀으로 회원가입할 수 없다")
    void rejectsUnknownTeamOnSignUp() {
        assertBusinessException(ErrorCode.TEAM_NOT_FOUND,
                () -> userService.signUp(new UserCreateRequest(999999L, "service-unknown-team@example.test", "password1234!", "팀 오류 사용자")));
        log.info("회원가입 소속 팀 존재 여부 검증 완료");
    }

    @Test
    @DisplayName("회원 ID로 회원을 조회한다")
    void getsUser() {
        User user = userRepository.saveAndFlush(new User(null, "service-get@example.test", "encoded-password", "조회 사용자", UserRole.USER, UserStatus.ACTIVE));

        UserResponse response = userService.getUser(user.getId());

        assertThat(response.email()).isEqualTo("service-get@example.test");
        log.info("회원 조회 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회할 수 없다")
    void rejectsUnknownUserOnGet() {
        assertBusinessException(ErrorCode.USER_NOT_FOUND, () -> userService.getUser(999999L));
        log.info("회원 조회 존재 여부 검증 완료");
    }

    @Test
    @DisplayName("회원 이름과 소속 팀을 수정한다")
    void updatesUserNameAndTeam() {
        User user = userRepository.saveAndFlush(new User(null, "service-update@example.test", "encoded-password", "수정 전 사용자", UserRole.USER, UserStatus.ACTIVE));
        Team team = teamRepository.saveAndFlush(new Team("service-update-team", "회원 수정 테스트 팀"));

        UserResponse response = userService.updateUser(user.getId(), new UserUpdateRequest(team.getId(), "수정 후 사용자"));

        assertThat(response.name()).isEqualTo("수정 후 사용자");
        assertThat(response.teamId()).isEqualTo(team.getId());
        log.info("회원 이름 및 소속 팀 수정 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("회원의 소속 팀을 해제한다")
    void clearsUserTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-clear-team", "소속 해제 테스트 팀"));
        User user = userRepository.saveAndFlush(new User(team.getId(), "service-clear-team@example.test", "encoded-password", "소속 해제 사용자", UserRole.USER, UserStatus.ACTIVE));

        UserResponse response = userService.updateUser(user.getId(), new UserUpdateRequest(null, "소속 해제 사용자"));

        assertThat(response.teamId()).isNull();
        log.info("회원 소속 팀 해제 서비스 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("회원 수정 시 존재하지 않는 팀을 지정할 수 없다")
    void rejectsUnknownTeamOnUpdate() {
        User user = userRepository.saveAndFlush(new User(null, "service-update-team-error@example.test", "encoded-password", "팀 오류 사용자", UserRole.USER, UserStatus.ACTIVE));

        assertBusinessException(ErrorCode.TEAM_NOT_FOUND,
                () -> userService.updateUser(user.getId(), new UserUpdateRequest(999999L, "수정 사용자")));
        log.info("회원 수정 소속 팀 존재 여부 검증 완료: userId={}", user.getId());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 수정할 수 없다")
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
