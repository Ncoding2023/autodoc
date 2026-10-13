package com.autodoc.domain.user.dto;

import com.autodoc.domain.team.dto.TeamCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        Long teamId,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String name,
        @Valid TeamCreateRequest team
) {
    public UserCreateRequest(Long teamId, String email, String password, String name) {
        this(teamId, email, password, name, null);
    }

    @AssertTrue(message = "기존 팀 선택과 새 팀 등록은 동시에 사용할 수 없습니다.")
    public boolean isTeamSelectionValid() {
        return teamId == null || team == null;
    }
}
