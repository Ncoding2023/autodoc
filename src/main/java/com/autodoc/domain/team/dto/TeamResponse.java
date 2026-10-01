package com.autodoc.domain.team.dto;

import com.autodoc.domain.team.Team;

public record TeamResponse(Long id, String name, String description) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getDescription());
    }
}
