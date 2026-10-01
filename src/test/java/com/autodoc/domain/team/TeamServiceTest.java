package com.autodoc.domain.team;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.team.dto.TeamCreateRequest;
import com.autodoc.domain.team.dto.TeamResponse;
import com.autodoc.domain.team.dto.TeamUpdateRequest;
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
class TeamServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TeamServiceTest.class);

    @Autowired private TeamService teamService;
    @Autowired private TeamRepository teamRepository;

    @Test
    void createsTeam() {
        TeamResponse response = teamService.createTeam(new TeamCreateRequest("service-create-team", "팀 생성 설명"));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("service-create-team");
        log.info("팀 생성 서비스 검증 완료: teamId={}", response.id());
    }

    @Test
    void rejectsDuplicateTeamNameOnCreate() {
        teamRepository.saveAndFlush(new Team("service-duplicate-team", "기존 팀"));

        assertBusinessException(ErrorCode.DUPLICATE_TEAM_NAME,
                () -> teamService.createTeam(new TeamCreateRequest("service-duplicate-team", "중복 팀")));
        log.info("팀 생성 이름 중복 검증 완료");
    }

    @Test
    void getsTeamList() {
        teamRepository.saveAndFlush(new Team("service-list-team-1", "첫 번째 팀"));
        teamRepository.saveAndFlush(new Team("service-list-team-2", "두 번째 팀"));

        assertThat(teamService.getTeams()).extracting(TeamResponse::name)
                .contains("service-list-team-1", "service-list-team-2");
        log.info("팀 목록 조회 서비스 검증 완료");
    }

    @Test
    void getsTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-get-team", "팀 조회 설명"));

        TeamResponse response = teamService.getTeam(team.getId());

        assertThat(response.name()).isEqualTo("service-get-team");
        log.info("팀 상세 조회 서비스 검증 완료: teamId={}", team.getId());
    }

    @Test
    void rejectsUnknownTeamOnGet() {
        assertBusinessException(ErrorCode.TEAM_NOT_FOUND, () -> teamService.getTeam(999999L));
        log.info("팀 상세 조회 존재 여부 검증 완료");
    }

    @Test
    void updatesTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-update-team", "수정 전 설명"));

        TeamResponse response = teamService.updateTeam(team.getId(), new TeamUpdateRequest("service-updated-team", "수정 후 설명"));

        assertThat(response.name()).isEqualTo("service-updated-team");
        assertThat(response.description()).isEqualTo("수정 후 설명");
        log.info("팀 수정 서비스 검증 완료: teamId={}", team.getId());
    }

    @Test
    void rejectsDuplicateTeamNameOnUpdate() {
        Team target = teamRepository.saveAndFlush(new Team("service-update-target", "수정 대상"));
        teamRepository.saveAndFlush(new Team("service-update-duplicate", "중복 대상"));

        assertBusinessException(ErrorCode.DUPLICATE_TEAM_NAME,
                () -> teamService.updateTeam(target.getId(), new TeamUpdateRequest("service-update-duplicate", "수정 설명")));
        log.info("팀 수정 이름 중복 검증 완료: teamId={}", target.getId());
    }

    @Test
    void deletesTeam() {
        Team team = teamRepository.saveAndFlush(new Team("service-delete-team", "삭제 대상"));

        teamService.deleteTeam(team.getId());

        assertThat(teamRepository.findById(team.getId())).isEmpty();
        log.info("팀 삭제 서비스 검증 완료: teamId={}", team.getId());
    }

    @Test
    void rejectsUnknownTeamOnDelete() {
        assertBusinessException(ErrorCode.TEAM_NOT_FOUND, () -> teamService.deleteTeam(999999L));
        log.info("팀 삭제 대상 존재 여부 검증 완료");
    }

    private void assertBusinessException(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
