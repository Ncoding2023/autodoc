package com.autodoc.domain.team;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class TeamRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(TeamRepositoryTest.class);
    @Autowired private TeamRepository teamRepository;

    @Test void createsTeam() {
        Team saved = teamRepository.saveAndFlush(new Team("repository-create-team", "create description"));
        assertThat(saved.getId()).isNotNull();
        log.info("팀 생성 완료: id={}", saved.getId());
    }

    @Test void readsTeamByIdAndName() {
        Team saved = teamRepository.saveAndFlush(new Team("repository-read-team", "read description"));
        assertThat(teamRepository.findById(saved.getId())).isPresent();
        assertThat(teamRepository.findByName("repository-read-team")).isPresent();
        assertThat(teamRepository.findByName("unknown")).isEmpty();
        log.info("팀 조회 완료: id={}", saved.getId());
    }

    @Test void updatesTeam() {
        Team saved = teamRepository.saveAndFlush(new Team("repository-update-team", "initial description"));
        Team found = teamRepository.findById(saved.getId()).orElseThrow();
        found.update("repository-updated-team", "updated description");
        teamRepository.flush();
        assertThat(teamRepository.findByName("repository-updated-team")).get()
                .extracting(Team::getDescription).isEqualTo("updated description");
        log.info("팀 수정 완료: id={}", saved.getId());
    }

    @Test void deletesTeam() {
        Team saved = teamRepository.saveAndFlush(new Team("repository-delete-team", "delete description"));
        teamRepository.deleteById(saved.getId());
        teamRepository.flush();
        assertThat(teamRepository.findById(saved.getId())).isEmpty();
        log.info("팀 삭제 완료: id={}", saved.getId());
    }
}
