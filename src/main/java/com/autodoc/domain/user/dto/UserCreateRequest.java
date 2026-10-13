package com.autodoc.domain.user.dto;

import com.autodoc.domain.team.dto.TeamCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record UserCreateRequest(
        Long teamId,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = ".*[\\p{P}\\p{S}].*", flags = Pattern.Flag.DOTALL,
                message = "비밀번호는 특수문자를 하나 이상 포함해야 합니다.") String password,
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

    @AssertTrue(message = "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
