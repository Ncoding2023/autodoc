package com.autodoc.domain.team;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.team.dto.TeamCreateRequest;
import com.autodoc.domain.team.dto.TeamResponse;
import com.autodoc.domain.team.dto.TeamUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        if (teamRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME);
        }

        return TeamResponse.from(teamRepository.save(new Team(request.name(), request.description())));
    }

    public List<TeamResponse> getTeams() {
        return teamRepository.findAll().stream().map(TeamResponse::from).toList();
    }

    public TeamResponse getTeam(Long teamId) {
        return TeamResponse.from(findTeam(teamId));
    }

    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamUpdateRequest request) {
        Team team = findTeam(teamId);
        if (!team.getName().equals(request.name()) && teamRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME);
        }

        team.update(request.name(), request.description());
        return TeamResponse.from(team);
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        teamRepository.delete(findTeam(teamId));
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }
}
