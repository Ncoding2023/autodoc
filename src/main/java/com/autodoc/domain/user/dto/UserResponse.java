package com.autodoc.domain.user.dto;

import com.autodoc.domain.user.User;
import com.autodoc.domain.user.UserRole;
import com.autodoc.domain.user.UserStatus;

public record UserResponse(Long id, Long teamId, String email, String name, UserRole role, UserStatus status) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getTeamId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus());
    }
}
