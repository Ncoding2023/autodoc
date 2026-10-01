package com.autodoc.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        Long teamId,
        @NotBlank @Size(max = 100) String name
) {
}
